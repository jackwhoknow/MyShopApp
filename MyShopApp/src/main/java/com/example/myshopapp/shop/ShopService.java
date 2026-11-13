package com.example.myshopapp.shop;

import com.example.myshopapp.common.api.PageResult;
import com.example.myshopapp.common.exception.BusinessException;
import com.example.myshopapp.common.exception.ErrorCode;
import com.example.myshopapp.product.entity.ProductStatus;
import com.example.myshopapp.product.repository.ProductRepository;
import com.example.myshopapp.shop.dto.ShopApplyRequest;
import com.example.myshopapp.shop.dto.ShopPublicVO;
import com.example.myshopapp.shop.dto.ShopVO;
import com.example.myshopapp.shop.entity.Shop;
import com.example.myshopapp.shop.entity.ShopStatus;
import com.example.myshopapp.shop.repository.ShopFollowRepository;
import com.example.myshopapp.shop.repository.ShopRepository;
import com.example.myshopapp.user.entity.User;
import com.example.myshopapp.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 店铺业务：商家提交申请，平台审核。
 */
@Service
@RequiredArgsConstructor
public class ShopService {

    private final ShopRepository shopRepository;
    private final ShopFollowRepository shopFollowRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @Transactional
    public ShopVO apply(Long ownerId, ShopApplyRequest request) {
        // 已有待审核或营业中的店铺时不允许重复申请；被驳回后可以重新提交
        if (shopRepository.existsByOwnerIdAndStatusIn(ownerId,
                List.of(ShopStatus.PENDING, ShopStatus.ACTIVE))) {
            throw new BusinessException(ErrorCode.CONFLICT, "已存在待审核或营业中的店铺");
        }

        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "用户不存在"));

        Shop shop = new Shop();
        shop.setOwner(owner);
        shop.setName(request.name());
        shop.setDescription(request.description());
        shop.setStatus(ShopStatus.PENDING);
        shopRepository.save(shop);

        return ShopVO.from(shop);
    }

    @Transactional(readOnly = true)
    public ShopVO getMine(Long ownerId) {
        Shop shop = shopRepository.findFirstByOwnerIdOrderByIdDesc(ownerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "还没有提交开店申请"));
        return ShopVO.from(shop);
    }

    /**
     * 店铺主页：仅营业中的店铺对外可见，未通过审核或已关闭的店铺返回 404。
     */
    @Transactional(readOnly = true)
    public ShopPublicVO publicDetail(Long shopId) {
        Shop shop = shopRepository.findById(shopId)
                .filter(item -> item.getStatus() == ShopStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "店铺不存在或已停止营业"));
        return new ShopPublicVO(
                shop.getId(),
                shop.getName(),
                shop.getDescription(),
                productRepository.countByShopIdAndStatus(shopId, ProductStatus.ON_SHELF),
                shopFollowRepository.countByShopId(shopId),
                shop.getCreatedAt());
    }

    @Transactional(readOnly = true)
    public PageResult<ShopVO> list(ShopStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        Page<Shop> shops = status == null
                ? shopRepository.findAll(pageable)
                : shopRepository.findByStatus(status, pageable);
        return PageResult.of(shops.map(ShopVO::from));
    }

    @Transactional
    public ShopVO approve(Long shopId) {
        Shop shop = requirePending(shopId);
        shop.setStatus(ShopStatus.ACTIVE);
        shop.setRejectReason(null);
        shop.setAuditedAt(LocalDateTime.now());
        return ShopVO.from(shop);
    }

    @Transactional
    public ShopVO reject(Long shopId, String reason) {
        Shop shop = requirePending(shopId);
        shop.setStatus(ShopStatus.REJECTED);
        shop.setRejectReason(reason);
        shop.setAuditedAt(LocalDateTime.now());
        return ShopVO.from(shop);
    }

    private Shop requirePending(Long shopId) {
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "店铺不存在"));
        if (shop.getStatus() != ShopStatus.PENDING) {
            throw new BusinessException(ErrorCode.CONFLICT, "只有待审核的店铺才能审核");
        }
        return shop;
    }
}
