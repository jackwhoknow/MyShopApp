package com.example.myshopapp.seckill;

import com.example.myshopapp.common.exception.BusinessException;
import com.example.myshopapp.common.exception.ErrorCode;
import com.example.myshopapp.order.repository.OrderItemRepository;
import com.example.myshopapp.product.entity.ProductSku;
import com.example.myshopapp.product.entity.ProductStatus;
import com.example.myshopapp.product.repository.ProductSkuRepository;
import com.example.myshopapp.seckill.dto.SeckillActivityCreateRequest;
import com.example.myshopapp.seckill.dto.SeckillActivityVO;
import com.example.myshopapp.seckill.entity.SeckillActivity;
import com.example.myshopapp.seckill.repository.SeckillActivityRepository;
import com.example.myshopapp.shop.entity.Shop;
import com.example.myshopapp.shop.entity.ShopStatus;
import com.example.myshopapp.shop.repository.ShopRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 秒杀活动业务：商家管理活动、买家浏览活动、下单按活动价结算并校验每人限购。
 */
@Service
@RequiredArgsConstructor
public class SeckillActivityService {

    private final SeckillActivityRepository activityRepository;
    private final ProductSkuRepository productSkuRepository;
    private final ShopRepository shopRepository;
    private final OrderItemRepository orderItemRepository;

    /** 首页秒杀专区：进行中 + 即将开始 */
    @Transactional(readOnly = true)
    public List<SeckillActivityVO> publicList() {
        LocalDateTime now = LocalDateTime.now();
        return activityRepository.findUpcomingOrRunning(now, ProductStatus.ON_SHELF, ShopStatus.ACTIVE)
                .stream()
                .map(activity -> SeckillActivityVO.from(activity, now))
                .toList();
    }

    /** 商家活动列表 */
    @Transactional(readOnly = true)
    public List<SeckillActivityVO> merchantList(Long ownerId) {
        Shop shop = requireShop(ownerId);
        LocalDateTime now = LocalDateTime.now();
        return activityRepository.findByShopIdOrderByIdDesc(shop.getId()).stream()
                .map(activity -> SeckillActivityVO.from(activity, now))
                .toList();
    }

    @Transactional
    public SeckillActivityVO create(Long ownerId, SeckillActivityCreateRequest request) {
        Shop shop = requireShop(ownerId);
        ProductSku sku = productSkuRepository.findById(request.skuId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "商品规格不存在"));
        if (!sku.getProduct().getShop().getId().equals(shop.getId())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "只能为本店铺商品创建秒杀活动");
        }
        if (request.activityPrice().compareTo(sku.getPrice()) >= 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "活动价必须低于商品原价");
        }
        if (!request.endAt().isAfter(request.startAt())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "结束时间必须晚于开始时间");
        }
        if (!request.endAt().isAfter(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "结束时间必须晚于当前时间");
        }
        if (request.totalStock() > sku.getStock()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR,
                    "活动库存不能超过当前商品库存（" + sku.getStock() + "）");
        }
        if (request.perUserLimit() > request.totalStock()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "每人限购不能超过活动库存");
        }
        if (activityRepository.existsOverlapping(sku.getId(), request.startAt(), request.endAt(), -1L)) {
            throw new BusinessException(ErrorCode.CONFLICT, "该商品在所选时间段已有秒杀活动");
        }

        SeckillActivity activity = new SeckillActivity();
        activity.setShop(shop);
        activity.setProduct(sku.getProduct());
        activity.setSku(sku);
        activity.setActivityPrice(request.activityPrice());
        activity.setTotalStock(request.totalStock());
        activity.setSoldCount(0);
        activity.setPerUserLimit(request.perUserLimit());
        activity.setStartAt(request.startAt());
        activity.setEndAt(request.endAt());
        activity.setEnabled(true);
        activityRepository.save(activity);

        return SeckillActivityVO.from(activity, LocalDateTime.now());
    }

    @Transactional
    public SeckillActivityVO updateStatus(Long ownerId, Long activityId, boolean enabled) {
        Shop shop = requireShop(ownerId);
        SeckillActivity activity = activityRepository.findByIdAndShopId(activityId, shop.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "秒杀活动不存在"));
        if (enabled && activityRepository.existsOverlapping(activity.getSku().getId(),
                activity.getStartAt(), activity.getEndAt(), activity.getId())) {
            throw new BusinessException(ErrorCode.CONFLICT, "该商品在所选时间段已有其他秒杀活动");
        }
        activity.setEnabled(enabled);
        return SeckillActivityVO.from(activity, LocalDateTime.now());
    }

    /** 当前正在进行的活动：skuId -> 活动，下单与购物车展示共用 */
    @Transactional(readOnly = true)
    public Map<Long, SeckillActivity> runningBySku() {
        return runningBySkuIds(null);
    }

    /**
     * 查询指定 SKU 正在进行的活动。
     * skuIds 为空表示查询全部（活动数量少，用于整单结算）。
     */
    @Transactional(readOnly = true)
    public Map<Long, SeckillActivity> runningBySkuIds(Collection<Long> skuIds) {
        LocalDateTime now = LocalDateTime.now();
        List<SeckillActivity> activities = skuIds == null
                ? activityRepository.findRunning(now, ProductStatus.ON_SHELF, ShopStatus.ACTIVE)
                : activityRepository.findRunningBySkuIds(skuIds, now);
        return activities.stream()
                .collect(Collectors.toMap(activity -> activity.getSku().getId(),
                        Function.identity(), (first, second) -> first));
    }

    /**
     * 下单时校验并扣减活动库存：加锁后依次校验活动状态、活动库存与每人限购，
     * 返回带活动价的活动实体，由调用方写入订单项快照。
     */
    @Transactional
    public SeckillActivity consume(Long userId, Long activityId, int quantity) {
        SeckillActivity activity = activityRepository.findByIdForUpdate(activityId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONFLICT, "秒杀活动已结束"));
        LocalDateTime now = LocalDateTime.now();
        if (!activity.isEnabled()
                || now.isBefore(activity.getStartAt())
                || !now.isBefore(activity.getEndAt())) {
            throw new BusinessException(ErrorCode.CONFLICT, "秒杀活动已结束");
        }
        if (activity.getSoldCount() + quantity > activity.getTotalStock()) {
            throw new BusinessException(ErrorCode.CONFLICT, "秒杀商品已售罄");
        }
        long bought = orderItemRepository.sumSeckillQuantity(activityId, userId);
        if (bought + quantity > activity.getPerUserLimit()) {
            throw new BusinessException(ErrorCode.CONFLICT,
                    "秒杀商品每人限购 " + activity.getPerUserLimit() + " 件");
        }
        activity.setSoldCount(activity.getSoldCount() + quantity);
        return activity;
    }

    /** 取消订单时回补活动库存 */
    @Transactional
    public void restore(Long activityId, int quantity) {
        activityRepository.findByIdForUpdate(activityId)
                .ifPresent(activity -> activity.setSoldCount(
                        Math.max(activity.getSoldCount() - quantity, 0)));
    }

    private Shop requireShop(Long ownerId) {
        return shopRepository.findFirstByOwnerIdOrderByIdDesc(ownerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "还没有店铺，无法管理秒杀活动"));
    }
}
