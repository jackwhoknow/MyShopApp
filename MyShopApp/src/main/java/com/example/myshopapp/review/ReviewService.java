package com.example.myshopapp.review;

import com.example.myshopapp.common.api.PageResult;
import com.example.myshopapp.common.exception.BusinessException;
import com.example.myshopapp.common.exception.ErrorCode;
import com.example.myshopapp.order.entity.OrderItem;
import com.example.myshopapp.order.entity.SubOrder;
import com.example.myshopapp.order.entity.SubOrderStatus;
import com.example.myshopapp.order.repository.OrderItemRepository;
import com.example.myshopapp.review.dto.ReviewAppendRequest;
import com.example.myshopapp.review.dto.ReviewRequest;
import com.example.myshopapp.review.dto.ReviewVO;
import com.example.myshopapp.review.entity.Review;
import com.example.myshopapp.review.entity.ReviewStatus;
import com.example.myshopapp.review.repository.ReviewRepository;
import com.example.myshopapp.shop.entity.Shop;
import com.example.myshopapp.shop.repository.ShopRepository;
import com.example.myshopapp.upload.ImageUrlPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 评价：买家对已完成订单的商品评价，商家查看并回复。
 */
@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final OrderItemRepository orderItemRepository;
    private final ShopRepository shopRepository;
    private final ReviewContentFilter contentFilter;
    private final ImageUrlPolicy imageUrlPolicy;

    /** 买家评价：订单商品必须属于本人、所在子订单已完成、且未评价过 */
    @Transactional
    public ReviewVO create(Long userId, ReviewRequest request) {
        OrderItem item = orderItemRepository.findById(request.orderItemId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "订单商品不存在"));
        SubOrder subOrder = item.getSubOrder();

        if (!subOrder.getOrder().getUser().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "订单商品不存在");
        }
        if (subOrder.getStatus() != SubOrderStatus.COMPLETED) {
            throw new BusinessException(ErrorCode.CONFLICT, "订单完成后才能评价");
        }
        if (reviewRepository.existsByOrderItemId(item.getId())) {
            throw new BusinessException(ErrorCode.CONFLICT, "该商品已评价");
        }

        Review review = new Review();
        review.setOrderItem(item);
        review.setUserId(userId);
        review.setShopId(subOrder.getShop().getId());
        review.setProductId(item.getProductId());
        review.setRating(request.rating());
        review.setContent(request.content() == null ? null : request.content().trim());
        review.setImages(normalizeImages(request.images()));
        // 命中敏感词转人工审核，正常评价直接通过
        review.setStatus(contentFilter.containsSensitive(review.getContent())
                ? ReviewStatus.PENDING : ReviewStatus.APPROVED);
        return ReviewVO.from(reviewRepository.save(review));
    }

    /** 追评：只能追加自己的评价，且每笔评价只能追评一次；屏蔽词追评转人工审核 */
    @Transactional
    public ReviewVO append(Long userId, Long reviewId, ReviewAppendRequest request) {
        Review review = reviewRepository.findByIdAndUserId(reviewId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "评价不存在"));
        if (review.getStatus() == ReviewStatus.REJECTED) {
            throw new BusinessException(ErrorCode.CONFLICT, "该评价已被驳回，不能追评");
        }
        if (review.getAppendedAt() != null) {
            throw new BusinessException(ErrorCode.CONFLICT, "该评价已经追评过了");
        }
        String content = request.content() == null ? null : request.content().trim();
        String images = normalizeImages(request.images());
        if ((content == null || content.isBlank()) && images == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "请填写追评内容或上传图片");
        }
        review.setAppendContent(content == null || content.isBlank() ? null : content);
        review.setAppendImages(images);
        review.setAppendedAt(LocalDateTime.now());
        if (contentFilter.containsSensitive(content)) {
            review.setStatus(ReviewStatus.PENDING);
        }
        return ReviewVO.from(review);
    }

    /** 只接受本站上传接口返回的图片地址，避免把外部链接写入评价 */
    private String normalizeImages(java.util.List<String> images) {
        if (images == null || images.isEmpty()) {
            return null;
        }
        java.util.List<String> valid = images.stream()
                .filter(imageUrlPolicy::allowed)
                .distinct()
                .toList();
        return valid.isEmpty() ? null : String.join(",", valid);
    }

    /** 商品页评价列表：仅审核通过的评价对外展示 */
    @Transactional(readOnly = true)
    public PageResult<ReviewVO> listForProduct(Long productId, int page, int size) {
        Page<Review> result = reviewRepository.findByProductIdAndStatusOrderByIdDesc(
                productId, ReviewStatus.APPROVED, PageRequest.of(page, size));
        return PageResult.of(result.map(ReviewVO::from));
    }

    /** 平台审核列表：status 为空查全部，keyword 匹配评价内容或商品名 */
    @Transactional(readOnly = true)
    public PageResult<ReviewVO> listForAdmin(ReviewStatus status, String keyword, int page, int size) {
        String normalized = keyword == null || keyword.isBlank() ? null : keyword.trim();
        Page<Review> result = reviewRepository.searchForAdmin(
                status, normalized, PageRequest.of(page, size));
        return PageResult.of(result.map(ReviewVO::from));
    }

    @Transactional
    public ReviewVO approve(Long reviewId) {
        return audit(reviewId, ReviewStatus.APPROVED, null);
    }

    @Transactional
    public ReviewVO reject(Long reviewId, String remark) {
        return audit(reviewId, ReviewStatus.REJECTED, remark);
    }

    private ReviewVO audit(Long reviewId, ReviewStatus status, String remark) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "评价不存在"));
        review.setStatus(status);
        review.setAuditRemark(remark == null || remark.isBlank() ? null : remark.trim());
        return ReviewVO.from(review);
    }

    @Transactional(readOnly = true)
    public PageResult<ReviewVO> listMine(Long userId, int page, int size) {
        Page<Review> result = reviewRepository.findByUserIdOrderByIdDesc(userId, PageRequest.of(page, size));
        return PageResult.of(result.map(ReviewVO::from));
    }

    @Transactional(readOnly = true)
    public PageResult<ReviewVO> listForShop(Long ownerId, int page, int size) {
        Shop shop = requireShop(ownerId);
        Page<Review> result = reviewRepository.findByShopIdOrderByIdDesc(shop.getId(), PageRequest.of(page, size));
        return PageResult.of(result.map(ReviewVO::from));
    }

    /** 商家回复：只能回复本店评价，且只能回复一次 */
    @Transactional
    public ReviewVO reply(Long ownerId, Long reviewId, String reply) {
        Shop shop = requireShop(ownerId);
        Review review = reviewRepository.findByIdAndShopId(reviewId, shop.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "评价不存在"));
        if (review.getReply() != null) {
            throw new BusinessException(ErrorCode.CONFLICT, "该评价已回复");
        }
        review.setReply(reply.trim());
        review.setRepliedAt(LocalDateTime.now());
        return ReviewVO.from(review);
    }

    private Shop requireShop(Long ownerId) {
        return shopRepository.findFirstByOwnerIdOrderByIdDesc(ownerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "还没有店铺，无法管理评价"));
    }
}
