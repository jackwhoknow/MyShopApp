package com.example.myshopapp.product;

import com.example.myshopapp.cart.repository.CartItemRepository;
import com.example.myshopapp.category.entity.Category;
import com.example.myshopapp.category.repository.CategoryRepository;
import com.example.myshopapp.common.api.PageResult;
import com.example.myshopapp.common.exception.BusinessException;
import com.example.myshopapp.common.exception.ErrorCode;
import com.example.myshopapp.favorite.repository.ProductFavoriteRepository;
import com.example.myshopapp.history.repository.BrowseHistoryRepository;
import com.example.myshopapp.order.repository.OrderItemRepository;
import com.example.myshopapp.order.seckill.SeckillStockService;
import com.example.myshopapp.product.dto.BatchProductStatusRequest;
import com.example.myshopapp.product.dto.BatchProductStatusResultVO;
import com.example.myshopapp.product.dto.ProductCreateRequest;
import com.example.myshopapp.product.dto.ProductDetailVO;
import com.example.myshopapp.product.dto.ProductListItemVO;
import com.example.myshopapp.product.dto.ProductUpdateRequest;
import com.example.myshopapp.product.dto.SkuStockUpdateRequest;
import com.example.myshopapp.product.entity.Product;
import com.example.myshopapp.product.entity.ProductSku;
import com.example.myshopapp.product.entity.ProductSpec;
import com.example.myshopapp.product.entity.ProductSpecValue;
import com.example.myshopapp.product.entity.ProductStatus;
import com.example.myshopapp.product.repository.ProductRepository;
import com.example.myshopapp.shop.entity.Shop;
import com.example.myshopapp.shop.entity.ShopStatus;
import com.example.myshopapp.shop.repository.ShopRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 商品业务：商家维护商品与 SKU，前台浏览已上架商品。
 */
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ShopRepository shopRepository;
    private final CategoryRepository categoryRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductFavoriteRepository productFavoriteRepository;
    private final BrowseHistoryRepository browseHistoryRepository;
    private final SeckillStockService seckillStockService;

    @Transactional
    public ProductDetailVO create(Long ownerId, ProductCreateRequest request) {
        Shop shop = requireActiveShop(ownerId);
        Category category = requireCategory(request.categoryId());
        validateSpecRequest(request);

        Product product = new Product();
        product.setShop(shop);
        product.setCategory(category);
        product.setName(request.name());
        product.setSubtitle(request.subtitle());
        product.setDescription(request.description());
        product.setMainImage(request.mainImage());
        product.setPurchaseLimit(request.purchaseLimit() == null ? 0 : request.purchaseLimit());
        // 新建即提交审核，管理员通过后才对前台可见
        product.setStatus(ProductStatus.PENDING_REVIEW);

        // 规格名 -> 规格对象，创建 SKU 时要按规格名找到对应规格值
        Map<String, ProductSpec> specByName = new LinkedHashMap<>();
        for (int i = 0; i < request.specs().size(); i++) {
            ProductCreateRequest.SpecRequest specRequest = request.specs().get(i);
            ProductSpec spec = new ProductSpec();
            spec.setProduct(product);
            spec.setName(specRequest.name());
            spec.setSortOrder(i);
            for (int j = 0; j < specRequest.values().size(); j++) {
                ProductSpecValue value = new ProductSpecValue();
                value.setSpec(spec);
                value.setValue(specRequest.values().get(j));
                value.setSortOrder(j);
                spec.getValues().add(value);
            }
            product.getSpecs().add(spec);
            specByName.put(spec.getName(), spec);
        }

        Set<String> skuSignatures = new HashSet<>();
        for (ProductCreateRequest.SkuRequest skuRequest : request.skus()) {
            ProductSku sku = new ProductSku();
            sku.setProduct(product);
            sku.setPrice(skuRequest.price());
            sku.setStock(skuRequest.stock());
            sku.setImage(skuRequest.image());

            List<String> signature = new ArrayList<>();
            for (ProductSpec spec : specByName.values()) {
                String chosen = skuRequest.specValues().get(spec.getName());
                if (!StringUtils.hasText(chosen)) {
                    throw new BusinessException(ErrorCode.PARAM_ERROR,
                            "SKU 缺少规格「" + spec.getName() + "」的取值");
                }
                ProductSpecValue value = spec.getValues().stream()
                        .filter(v -> v.getValue().equals(chosen))
                        .findFirst()
                        .orElseThrow(() -> new BusinessException(ErrorCode.PARAM_ERROR,
                                "规格「" + spec.getName() + "」没有取值：" + chosen));
                sku.getSpecValues().add(value);
                signature.add(chosen);
            }
            if (!skuSignatures.add(String.join("|", signature))) {
                throw new BusinessException(ErrorCode.PARAM_ERROR,
                        "存在重复的 SKU 规格组合：" + String.join("/", signature));
            }
            product.getSkus().add(sku);
        }

        applyPriceRange(product);
        productRepository.save(product);
        return ProductDetailVO.from(product);
    }

    @Transactional
    @CacheEvict(cacheNames = "product", key = "#productId")
    public ProductDetailVO update(Long ownerId, Long productId, ProductUpdateRequest request) {
        Product product = requireOwnedProduct(ownerId, productId);
        if (request.categoryId() != null) {
            product.setCategory(requireCategory(request.categoryId()));
        }
        product.setName(request.name());
        product.setSubtitle(request.subtitle());
        product.setDescription(request.description());
        product.setMainImage(request.mainImage());
        if (request.purchaseLimit() != null) {
            product.setPurchaseLimit(request.purchaseLimit());
        }

        // 规格和 SKU 要么都不提交（只改基础信息），要么一起提交做全量替换
        if ((request.specs() == null) != (request.skus() == null)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "修改规格时必须同时提交规格与 SKU");
        }
        if (request.specs() != null) {
            List<SpecInput> specs = request.specs().stream()
                    .map(spec -> new SpecInput(spec.name(), spec.values()))
                    .toList();
            List<SkuInput> skus = request.skus().stream()
                    .map(sku -> new SkuInput(sku.id(), sku.price(), sku.stock(),
                            sku.image(), sku.specValues()))
                    .toList();
            replaceSpecsAndSkus(product, specs, skus);
            applyPriceRange(product);
            productRepository.flush();
        }
        // 内容变更后需要重新审核
        product.setStatus(ProductStatus.PENDING_REVIEW);
        product.setRejectReason(null);
        return ProductDetailVO.from(product);
    }

    /** 只维护 SKU 的价格与库存，保存后重算商品价格区间 */
    @Transactional
    @CacheEvict(cacheNames = "product", key = "#productId")
    public ProductDetailVO updateSkus(Long ownerId, Long productId, SkuStockUpdateRequest request) {
        Product product = requireOwnedProduct(ownerId, productId);
        Map<Long, ProductSku> skusById = product.getSkus().stream()
                .collect(Collectors.toMap(ProductSku::getId, sku -> sku));

        Set<Long> submitted = new HashSet<>();
        for (SkuStockUpdateRequest.Item item : request.skus()) {
            if (!submitted.add(item.skuId())) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "SKU 重复提交：" + item.skuId());
            }
            ProductSku sku = skusById.get(item.skuId());
            if (sku == null) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "SKU 不属于该商品：" + item.skuId());
            }
            sku.setPrice(item.price());
            seckillStockService.adjustAfterCommit(item.skuId(), item.stock() - sku.getStock());
            sku.setStock(item.stock());
        }
        applyPriceRange(product);
        return ProductDetailVO.from(product);
    }

    /** 删除商品：有订单引用时拒绝，否则先清理购物车与规格关联再删除 */
    @Transactional
    @CacheEvict(cacheNames = "product", key = "#productId")
    public void delete(Long ownerId, Long productId) {
        Product product = requireOwnedProduct(ownerId, productId);
        if (orderItemRepository.existsByProductId(productId)) {
            throw new BusinessException(ErrorCode.CONFLICT, "商品已被订单引用，不能删除，请先下架");
        }
        List<Long> skuIds = product.getSkus().stream()
                .map(ProductSku::getId)
                .toList();
        if (!skuIds.isEmpty()) {
            cartItemRepository.deleteBySkuIdIn(skuIds);
        }
        productFavoriteRepository.deleteByProductId(productId);
        browseHistoryRepository.deleteByProductId(productId);
        // 先断开 SKU 与规格值的关联并落库，避免删除规格值时触发外键约束
        product.getSkus().forEach(sku -> sku.getSpecValues().clear());
        productRepository.flush();
        productRepository.delete(product);
    }

    /** 批量上下架：逐个校验归属，失败项记录后继续处理其余商品 */
    @Transactional
    @CacheEvict(cacheNames = "product", allEntries = true)
    public BatchProductStatusResultVO batchChangeStatus(Long ownerId, BatchProductStatusRequest request) {
        if (request.status() != ProductStatus.ON_SHELF && request.status() != ProductStatus.OFF_SHELF) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "只支持提交审核或下架");
        }
        if (request.status() == ProductStatus.ON_SHELF) {
            requireActiveShop(ownerId);
        }
        List<Long> productIds = request.productIds().stream().distinct().toList();
        List<BatchProductStatusResultVO.Failure> failures = new ArrayList<>();
        int updated = 0;
        for (Long productId : productIds) {
            Product product = productRepository.findById(productId).orElse(null);
            if (product == null) {
                failures.add(new BatchProductStatusResultVO.Failure(productId, null, "商品不存在"));
                continue;
            }
            if (!product.getShop().getOwner().getId().equals(ownerId)) {
                failures.add(new BatchProductStatusResultVO.Failure(
                        productId, product.getName(), "无权操作其他店铺的商品"));
                continue;
            }
            applyStatusTransition(product, request.status());
            updated++;
        }
        return new BatchProductStatusResultVO(productIds.size(), updated, failures);
    }

    @Transactional
    @CacheEvict(cacheNames = "product", key = "#productId")
    public ProductDetailVO changeStatus(Long ownerId, Long productId, ProductStatus status) {
        Product product = requireOwnedProduct(ownerId, productId);
        if (status == ProductStatus.ON_SHELF) {
            requireActiveShop(ownerId);
        }
        applyStatusTransition(product, status);
        return ProductDetailVO.from(product);
    }

    /** 商家侧状态流转：上架请求转为提交审核，下架直接生效 */
    private void applyStatusTransition(Product product, ProductStatus status) {
        if (status == ProductStatus.ON_SHELF) {
            product.setStatus(ProductStatus.PENDING_REVIEW);
            product.setRejectReason(null);
            return;
        }
        if (status == ProductStatus.OFF_SHELF) {
            product.setStatus(ProductStatus.OFF_SHELF);
            return;
        }
        throw new BusinessException(ErrorCode.PARAM_ERROR, "只支持提交审核或下架");
    }

    /** 管理员审核列表，可按状态与关键词筛选 */
    @Transactional(readOnly = true)
    public PageResult<ProductListItemVO> adminList(String status, String keyword, int page, int size) {
        ProductStatus filter = null;
        if (StringUtils.hasText(status)) {
            try {
                filter = ProductStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "商品状态不合法：" + status);
            }
        }
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        Page<Product> products = productRepository.searchAdmin(
                filter, normalizeKeyword(keyword), pageable);
        return PageResult.of(products.map(ProductListItemVO::from));
    }

    /** 审核通过：仅待审核商品可以被通过 */
    @Transactional
    @CacheEvict(cacheNames = "product", key = "#productId")
    public ProductDetailVO approve(Long productId) {
        Product product = requirePendingProduct(productId);
        product.setStatus(ProductStatus.ON_SHELF);
        product.setRejectReason(null);
        return ProductDetailVO.from(product);
    }

    /** 审核驳回：记录驳回理由，商品转为已下架 */
    @Transactional
    @CacheEvict(cacheNames = "product", key = "#productId")
    public ProductDetailVO reject(Long productId, String reason) {
        Product product = requirePendingProduct(productId);
        product.setStatus(ProductStatus.OFF_SHELF);
        product.setRejectReason(reason.trim());
        return ProductDetailVO.from(product);
    }

    private Product requirePendingProduct(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "商品不存在"));
        if (product.getStatus() != ProductStatus.PENDING_REVIEW) {
            throw new BusinessException(ErrorCode.CONFLICT, "该商品不处于待审核状态");
        }
        return product;
    }

    @Transactional(readOnly = true)
    public PageResult<ProductListItemVO> merchantList(Long ownerId, String keyword, int page, int size) {
        Shop shop = shopRepository.findFirstByOwnerIdOrderByIdDesc(ownerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "还没有提交开店申请"));
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        Page<Product> products = productRepository.searchByShop(
                shop.getId(), normalizeKeyword(keyword), pageable);
        return PageResult.of(products.map(ProductListItemVO::from));
    }

    @Transactional(readOnly = true)
    public ProductDetailVO merchantDetail(Long ownerId, Long productId) {
        return ProductDetailVO.from(requireOwnedProduct(ownerId, productId));
    }

    /** 前台列表：只返回已上架商品；支持关键词、分类（含子分类）、价格区间与排序 */
    @Transactional(readOnly = true)
    public PageResult<ProductListItemVO> publicList(String keyword, Long categoryId,
                                                    BigDecimal minPrice, BigDecimal maxPrice,
                                                    String sort, int page, int size) {
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "最低价不能高于最高价");
        }
        Pageable pageable = PageRequest.of(page, size, resolveSort(sort));
        String kw = normalizeKeyword(keyword);

        Page<Product> products;
        if (categoryId == null) {
            products = productRepository.searchPublic(
                    ProductStatus.ON_SHELF, kw, minPrice, maxPrice, pageable);
        } else {
            Category category = requireCategory(categoryId);
            List<Long> categoryIds = new ArrayList<>();
            categoryIds.add(category.getId());
            categoryRepository.findByPathStartingWith(category.getPath() + "/")
                    .forEach(child -> categoryIds.add(child.getId()));
            products = productRepository.searchPublicInCategories(
                    ProductStatus.ON_SHELF, categoryIds, kw, minPrice, maxPrice, pageable);
        }
        return PageResult.of(products.map(ProductListItemVO::from));
    }

    /** 店铺主页商品列表：只返回该店已上架商品，支持关键词与排序 */
    @Transactional(readOnly = true)
    public PageResult<ProductListItemVO> publicListByShop(Long shopId, String keyword, String sort,
                                                          int page, int size) {
        Pageable pageable = PageRequest.of(page, size, resolveSort(sort));
        Page<Product> products = productRepository.searchPublicByShop(
                shopId, ProductStatus.ON_SHELF, normalizeKeyword(keyword), pageable);
        return PageResult.of(products.map(ProductListItemVO::from));
    }

    /** 相关商品：同分类热销商品，最多 limit 个 */
    @Transactional(readOnly = true)
    public List<ProductListItemVO> related(Long productId, int limit) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "商品不存在"));
        return productRepository.findRelated(ProductStatus.ON_SHELF, product.getCategory().getId(),
                        productId, PageRequest.of(0, clampLimit(limit)))
                .stream().map(ProductListItemVO::from).toList();
    }

    /** 首页推荐：全站热销商品 */
    @Transactional(readOnly = true)
    public List<ProductListItemVO> recommended(int limit) {
        return productRepository.findRecommended(ProductStatus.ON_SHELF,
                        PageRequest.of(0, clampLimit(limit)))
                .stream().map(ProductListItemVO::from).toList();
    }

    private int clampLimit(int limit) {
        return Math.max(1, Math.min(limit, 20));
    }

    /** 排序规则：默认按最新 id，销量、价格、上架时间 */
    private Sort resolveSort(String sort) {
        if (sort == null) {
            return Sort.by(Sort.Direction.DESC, "id");
        }
        return switch (sort) {
            case "sales" -> Sort.by(Sort.Direction.DESC, "sales")
                    .and(Sort.by(Sort.Direction.DESC, "id"));
            case "priceAsc" -> Sort.by(Sort.Direction.ASC, "minPrice")
                    .and(Sort.by(Sort.Direction.DESC, "id"));
            case "priceDesc" -> Sort.by(Sort.Direction.DESC, "minPrice")
                    .and(Sort.by(Sort.Direction.DESC, "id"));
            case "newest" -> Sort.by(Sort.Direction.DESC, "createdAt");
            default -> Sort.by(Sort.Direction.DESC, "id");
        };
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "product", key = "#productId")
    public ProductDetailVO publicDetail(Long productId) {
        Product product = productRepository.findByIdAndStatus(productId, ProductStatus.ON_SHELF)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "商品不存在或已下架"));
        if (product.getShop().getStatus() != ShopStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "店铺已暂停营业");
        }
        return ProductDetailVO.from(product);
    }

    private void validateSpecRequest(ProductCreateRequest request) {
        Set<String> specNames = new HashSet<>();
        for (ProductCreateRequest.SpecRequest spec : request.specs()) {
            if (!specNames.add(spec.name())) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "规格名称不能重复：" + spec.name());
            }
            long distinctValues = spec.values().stream().distinct().count();
            if (distinctValues != spec.values().size()) {
                throw new BusinessException(ErrorCode.PARAM_ERROR,
                        "规格「" + spec.name() + "」存在重复取值");
            }
            if (spec.values().isEmpty()) {
                throw new BusinessException(ErrorCode.PARAM_ERROR,
                        "规格「" + spec.name() + "」至少需要一个取值");
            }
        }
        for (ProductCreateRequest.SkuRequest sku : request.skus()) {
            Set<String> missing = specNames.stream()
                    .filter(name -> !sku.specValues().containsKey(name))
                    .collect(Collectors.toSet());
            if (!missing.isEmpty()) {
                throw new BusinessException(ErrorCode.PARAM_ERROR,
                        "SKU 缺少规格：" + String.join(",", missing));
            }
        }
    }

    /** 全量替换规格与 SKU：同 id 的 SKU 复用更新，其余补建，未提交的删除 */
    private void replaceSpecsAndSkus(Product product, List<SpecInput> specs, List<SkuInput> skus) {
        validateSpecInputs(specs, skus);

        Map<Long, ProductSku> existingSkus = product.getSkus().stream()
                .collect(Collectors.toMap(ProductSku::getId, sku -> sku));
        Set<Long> requestedSkuIds = new HashSet<>();
        for (SkuInput sku : skus) {
            if (sku.id() == null) {
                continue;
            }
            if (!existingSkus.containsKey(sku.id())) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "SKU 不属于该商品：" + sku.id());
            }
            if (!requestedSkuIds.add(sku.id())) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "SKU 重复提交：" + sku.id());
            }
        }

        List<ProductSku> removedSkus = product.getSkus().stream()
                .filter(sku -> !requestedSkuIds.contains(sku.getId()))
                .toList();
        for (ProductSku sku : removedSkus) {
            if (orderItemRepository.existsBySkuId(sku.getId())) {
                throw new BusinessException(ErrorCode.CONFLICT, "SKU 已被订单引用，不能删除");
            }
        }

        // 先断开旧规格值关联并落库，再重建规格，避免删除规格值时触发外键约束
        product.getSkus().forEach(sku -> sku.getSpecValues().clear());
        product.getSkus().removeAll(removedSkus);
        product.getSpecs().clear();
        productRepository.flush();

        Map<String, ProductSpec> specByName = buildSpecs(product, specs);
        for (SkuInput input : skus) {
            ProductSku sku = input.id() == null ? new ProductSku() : existingSkus.get(input.id());
            if (input.id() != null) {
                // 已有 SKU 的库存可能被 Redis 预扣过，按差额同步避免秒杀读到旧库存
                seckillStockService.adjustAfterCommit(input.id(), input.stock() - sku.getStock());
            }
            sku.setProduct(product);
            sku.setPrice(input.price());
            sku.setStock(input.stock());
            sku.setImage(input.image());
            for (ProductSpec spec : specByName.values()) {
                String chosen = input.specValues().get(spec.getName());
                ProductSpecValue value = spec.getValues().stream()
                        .filter(v -> v.getValue().equals(chosen))
                        .findFirst()
                        .orElseThrow(() -> new BusinessException(ErrorCode.PARAM_ERROR,
                                "规格「" + spec.getName() + "」没有取值：" + chosen));
                sku.getSpecValues().add(value);
            }
            if (input.id() == null) {
                product.getSkus().add(sku);
            }
        }
    }

    /** 按提交顺序重建规格，返回“规格名 -> 规格对象”便于 SKU 绑定取值 */
    private Map<String, ProductSpec> buildSpecs(Product product, List<SpecInput> specs) {
        Map<String, ProductSpec> specByName = new LinkedHashMap<>();
        for (int i = 0; i < specs.size(); i++) {
            SpecInput specInput = specs.get(i);
            ProductSpec spec = new ProductSpec();
            spec.setProduct(product);
            spec.setName(specInput.name());
            spec.setSortOrder(i);
            for (int j = 0; j < specInput.values().size(); j++) {
                ProductSpecValue value = new ProductSpecValue();
                value.setSpec(spec);
                value.setValue(specInput.values().get(j));
                value.setSortOrder(j);
                spec.getValues().add(value);
            }
            product.getSpecs().add(spec);
            specByName.put(spec.getName(), spec);
        }
        return specByName;
    }

    /** 校验规格定义与 SKU 组合：规格不能重复、SKU 必须覆盖全部规格且组合不重复 */
    private void validateSpecInputs(List<SpecInput> specs, List<SkuInput> skus) {
        if (specs.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "至少定义一个规格");
        }
        if (skus.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "至少创建一个 SKU");
        }
        Set<String> specNames = new LinkedHashSet<>();
        Map<String, Set<String>> valuesByName = new LinkedHashMap<>();
        for (SpecInput spec : specs) {
            if (!StringUtils.hasText(spec.name())) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "规格名称不能为空");
            }
            if (!specNames.add(spec.name())) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "规格名称不能重复：" + spec.name());
            }
            Set<String> values = new HashSet<>();
            for (String value : spec.values()) {
                if (!StringUtils.hasText(value)) {
                    throw new BusinessException(ErrorCode.PARAM_ERROR,
                            "规格「" + spec.name() + "」的取值不能为空");
                }
                if (!values.add(value)) {
                    throw new BusinessException(ErrorCode.PARAM_ERROR,
                            "规格「" + spec.name() + "」存在重复取值");
                }
            }
            valuesByName.put(spec.name(), values);
        }

        Set<String> signatures = new HashSet<>();
        for (SkuInput sku : skus) {
            List<String> signature = new ArrayList<>();
            for (String name : specNames) {
                String chosen = sku.specValues().get(name);
                if (!StringUtils.hasText(chosen)) {
                    throw new BusinessException(ErrorCode.PARAM_ERROR,
                            "SKU 缺少规格「" + name + "」的取值");
                }
                if (!valuesByName.get(name).contains(chosen)) {
                    throw new BusinessException(ErrorCode.PARAM_ERROR,
                            "规格「" + name + "」没有取值：" + chosen);
                }
                signature.add(chosen);
            }
            if (!signatures.add(String.join("|", signature))) {
                throw new BusinessException(ErrorCode.PARAM_ERROR,
                        "存在重复的 SKU 规格组合：" + String.join("/", signature));
            }
        }
    }

    private void applyPriceRange(Product product) {
        BigDecimal min = product.getSkus().stream()
                .map(ProductSku::getPrice).min(BigDecimal::compareTo).orElse(null);
        BigDecimal max = product.getSkus().stream()
                .map(ProductSku::getPrice).max(BigDecimal::compareTo).orElse(null);
        product.setMinPrice(min);
        product.setMaxPrice(max);
    }

    private Shop requireActiveShop(Long ownerId) {
        Shop shop = shopRepository.findFirstByOwnerIdOrderByIdDesc(ownerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "还没有提交开店申请"));
        if (shop.getStatus() != ShopStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "店铺尚未通过审核，不能操作商品");
        }
        return shop;
    }

    private Product requireOwnedProduct(Long ownerId, Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "商品不存在"));
        if (!product.getShop().getOwner().getId().equals(ownerId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权操作其他店铺的商品");
        }
        return product;
    }

    private Category requireCategory(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "分类不存在"));
    }

    private String normalizeKeyword(String keyword) {
        return keyword == null ? "" : keyword.trim();
    }

    /** 内部规格输入，兼容创建与修改两种请求 */
    private record SpecInput(String name, List<String> values) {
    }

    private record SkuInput(Long id, BigDecimal price, Integer stock, String image,
                            Map<String, String> specValues) {
    }
}
