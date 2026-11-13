package com.example.myshopapp.invoice;

import com.example.myshopapp.common.api.PageResult;
import com.example.myshopapp.common.exception.BusinessException;
import com.example.myshopapp.common.exception.ErrorCode;
import com.example.myshopapp.invoice.dto.InvoiceRequest;
import com.example.myshopapp.invoice.dto.InvoiceVO;
import com.example.myshopapp.invoice.entity.Invoice;
import com.example.myshopapp.invoice.repository.InvoiceRepository;
import com.example.myshopapp.order.entity.Order;
import com.example.myshopapp.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Locale;

/**
 * 订单发票：买家对已支付订单提交开票申请，平台管理员开具。
 */
@Service
@RequiredArgsConstructor
public class InvoiceService {

    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_ISSUED = "ISSUED";

    private final InvoiceRepository invoiceRepository;
    private final OrderRepository orderRepository;

    @Transactional
    public InvoiceVO apply(Long userId, Long orderId, InvoiceRequest request) {
        Order order = orderRepository.findById(orderId)
                .filter(item -> item.getUser().getId().equals(userId))
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "订单不存在"));
        if (order.getPaidAt() == null) {
            throw new BusinessException(ErrorCode.CONFLICT, "订单支付后才能申请发票");
        }
        if (invoiceRepository.existsByOrderId(orderId)) {
            throw new BusinessException(ErrorCode.CONFLICT, "该订单已申请过发票");
        }
        String type = request.type().toUpperCase(Locale.ROOT);
        if ("COMPANY".equals(type) && !StringUtils.hasText(request.taxNo())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "单位发票必须填写税号");
        }

        Invoice invoice = new Invoice();
        invoice.setOrderId(orderId);
        invoice.setUserId(userId);
        invoice.setType(type);
        invoice.setTitle(request.title().trim());
        invoice.setTaxNo(request.taxNo());
        invoice.setEmail(request.email());
        invoice.setAmount(order.getTotalAmount());
        invoice.setStatus(STATUS_PENDING);
        return InvoiceVO.from(invoiceRepository.save(invoice));
    }

    @Transactional(readOnly = true)
    public InvoiceVO findByOrder(Long userId, Long orderId) {
        return invoiceRepository.findByOrderId(orderId)
                .filter(invoice -> invoice.getUserId().equals(userId))
                .map(InvoiceVO::from)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public PageResult<InvoiceVO> listMine(Long userId, int page, int size) {
        return PageResult.of(invoiceRepository.findByUserIdOrderByIdDesc(userId, PageRequest.of(page, size))
                .map(InvoiceVO::from));
    }

    @Transactional(readOnly = true)
    public PageResult<InvoiceVO> listForAdmin(String status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Invoice> result = StringUtils.hasText(status)
                ? invoiceRepository.findByStatusOrderByIdDesc(status.toUpperCase(Locale.ROOT), pageable)
                : invoiceRepository.findAllByOrderByIdDesc(pageable);
        return PageResult.of(result.map(InvoiceVO::from));
    }

    @Transactional
    public InvoiceVO issue(Long invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "发票申请不存在"));
        if (STATUS_ISSUED.equals(invoice.getStatus())) {
            throw new BusinessException(ErrorCode.CONFLICT, "该发票已开具");
        }
        invoice.setStatus(STATUS_ISSUED);
        invoice.setIssuedAt(LocalDateTime.now());
        return InvoiceVO.from(invoice);
    }
}
