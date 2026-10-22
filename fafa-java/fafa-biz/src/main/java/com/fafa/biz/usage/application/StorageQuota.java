package com.fafa.biz.usage.application;

import com.fafa.biz.identity.application.AccountAdministration;
import com.fafa.biz.usage.domain.QuotaAmounts;
import com.fafa.biz.usage.domain.QuotaRepository;
import com.fafa.biz.usage.domain.QuotaRepository.*;
import com.fafa.common.error.*;
import com.fafa.common.rpc.AdminBusinessRpc.QuotaView;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 存储计量应用端口；仅供可信业务用例调用，不向客户端暴露预占或结算写接口。 */
@Service
public class StorageQuota {
    private final AccountAdministration users;
    private final QuotaRepository quotas;
    private final QuotaPolicy policy;
    public StorageQuota(AccountAdministration users, QuotaRepository quotas, QuotaPolicy policy) {
        this.users = users; this.quotas = quotas; this.policy = policy;
    }
    @Transactional
    public QuotaView view(String owner) { return locked(owner).view(); }
    @Transactional
    public QuotaView grant(String owner, String operationId, long amount, String operator, String reason) {
        QuotaAmounts.operation(operationId); QuotaAmounts.reason(reason);
        if (amount <= 0) throw new FafaException(CommonErrorCode.INVALID_ARGUMENT);
        var balance = locked(owner);
        quotas.save(owner, new Balance(QuotaAmounts.add(balance.granted(), amount), balance.used(), balance.reserved(), balance.version()));
        quotas.ledger(owner, operationId, "GRANT", amount, operator, reason);
        return quotas.find(owner).view();
    }
    @Transactional
    public QuotaView reserve(String owner, String operationId, long bytes) {
        QuotaAmounts.operation(operationId);
        if (bytes <= 0) throw new FafaException(CommonErrorCode.INVALID_ARGUMENT);
        if (!"ACTIVE".equals(users.lock(owner).status())) throw new FafaException(CommonErrorCode.FORBIDDEN);
        var balance = locked(owner);
        var existing = quotas.reservation(operationId);
        if (existing != null) {
            matches(existing, owner, bytes);
            return balance.view();
        }
        if (bytes > balance.available()) throw new FafaException(BusinessError.QUOTA_INSUFFICIENT);
        quotas.reserve(operationId, owner, bytes);
        quotas.save(owner, new Balance(balance.granted(), balance.used(), balance.reserved() + bytes, balance.version()));
        quotas.ledger(owner, operationId, "RESERVE", bytes, null, "媒体上传预占");
        return quotas.find(owner).view();
    }
    @Transactional
    public QuotaView settle(String owner, String operationId, long actualBytes) {
        var balance = locked(owner);
        var reservation = requireReservation(owner, operationId);
        if (actualBytes < 0 || actualBytes > reservation.amount()) throw new FafaException(CommonErrorCode.INVALID_ARGUMENT);
        if ("SETTLED".equals(reservation.state()) && reservation.settledAmount() == actualBytes) return balance.view();
        if (!"RESERVED".equals(reservation.state())) conflict();
        quotas.save(owner, new Balance(balance.granted(), balance.used() + actualBytes,
                balance.reserved() - reservation.amount(), balance.version()));
        quotas.finish(operationId, "SETTLED", actualBytes);
        quotas.ledger(owner, operationId, "SETTLE", actualBytes, null, "按实际存储字节结算；剩余预占同步释放");
        return quotas.find(owner).view();
    }
    @Transactional
    public QuotaView release(String owner, String operationId) {
        var balance = locked(owner);
        var reservation = requireReservation(owner, operationId);
        if ("RELEASED".equals(reservation.state())) return balance.view();
        if (!"RESERVED".equals(reservation.state())) conflict();
        quotas.save(owner, new Balance(balance.granted(), balance.used(), balance.reserved() - reservation.amount(), balance.version()));
        quotas.finish(operationId, "RELEASED", 0);
        quotas.ledger(owner, operationId, "RELEASE", reservation.amount(), null, "取消或过期上传释放预占");
        return quotas.find(owner).view();
    }
    private Balance locked(String owner) {
        users.lock(owner);
        var balance = quotas.find(owner);
        if (balance == null) {
            quotas.create(owner, policy.initialStorageBytes());
            if (policy.initialStorageBytes() > 0) quotas.ledger(owner, "initial:" + owner, "GRANT", policy.initialStorageBytes(), null, "首次初始化存储政策");
            balance = quotas.find(owner);
        }
        return balance;
    }
    private Reservation requireReservation(String owner, String id) {
        QuotaAmounts.operation(id);
        var reservation = quotas.reservation(id);
        if (reservation == null || !owner.equals(reservation.userId())) throw new FafaException(CommonErrorCode.NOT_FOUND);
        return reservation;
    }
    private void matches(Reservation existing, String owner, long bytes) {
        if (!owner.equals(existing.userId()) || bytes != existing.amount() || !"RESERVED".equals(existing.state())) conflict();
    }
    private void conflict() { throw new FafaException(BusinessError.OPERATION_CONFLICT); }
}
