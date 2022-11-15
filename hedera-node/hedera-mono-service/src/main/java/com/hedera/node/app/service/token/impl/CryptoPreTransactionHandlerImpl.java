/*
 * Copyright (C) 2022 Hedera Hashgraph, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hedera.node.app.service.token.impl;

import static com.hedera.node.app.Utils.asHederaKey;
import static com.hederahashgraph.api.proto.java.ResponseCodeEnum.INVALID_ALLOWANCE_OWNER_ID;
import static com.hederahashgraph.api.proto.java.ResponseCodeEnum.INVALID_DELEGATING_SPENDER;
import static com.hederahashgraph.api.proto.java.ResponseCodeEnum.INVALID_TRANSFER_ACCOUNT_ID;

import com.hedera.node.app.SigTransactionMetadata;
import com.hedera.node.app.service.token.CryptoPreTransactionHandler;
import com.hedera.node.app.spi.key.HederaKey;
import com.hedera.node.app.spi.meta.TransactionMetadata;
import com.hederahashgraph.api.proto.java.AccountID;
import com.hederahashgraph.api.proto.java.TransactionBody;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nonnull;
import org.apache.commons.lang3.NotImplementedException;

/**
 * A {@code CryptoPreTransactionHandler} implementation that pre-computes the required signing keys
 * (but not the candidate signatures) for each crypto operation.
 */
public final class CryptoPreTransactionHandlerImpl implements CryptoPreTransactionHandler {
    private final AccountStore accountStore;

    public CryptoPreTransactionHandlerImpl(@Nonnull final AccountStore accountStore) {
        this.accountStore = Objects.requireNonNull(accountStore);
    }

    @Override
    /** {@inheritDoc} */
    public TransactionMetadata preHandleCryptoCreate(final TransactionBody tx) {
        final var op = tx.getCryptoCreateAccount();
        final var key = asHederaKey(op.getKey());
        final var receiverSigReq = op.getReceiverSigRequired();
        final var payer = tx.getTransactionID().getAccountID();
        return createAccountSigningMetadata(tx, key, receiverSigReq, payer);
    }

    @Override
    /** {@inheritDoc} */
    public TransactionMetadata preHandleCryptoDelete(TransactionBody txn) {
        final var op = txn.getCryptoDelete();
        final var payer = txn.getTransactionID().getAccountID();
        final var deleteAccountId = op.getDeleteAccountID();
        final var transferAccountId = op.getTransferAccountID();
        final var meta = new SigTransactionMetadata(accountStore, txn, payer);
        meta.addNonPayerKey(deleteAccountId);
        meta.addNonPayerKeyIfReceiverSigRequired(transferAccountId, INVALID_TRANSFER_ACCOUNT_ID);
        return meta;
    }

    @Override
    /** {@inheritDoc} */
    public TransactionMetadata preHandleApproveAllowances(TransactionBody txn) {
        final var op = txn.getCryptoApproveAllowance();
        final var payer = txn.getTransactionID().getAccountID();
        final var meta = new SigTransactionMetadata(accountStore, txn, payer);
        var failureStatus = INVALID_ALLOWANCE_OWNER_ID;

        for (final var allowance : op.getCryptoAllowancesList()) {
            meta.addNonPayerKey(allowance.getOwner(), failureStatus);
        }
        for (final var allowance : op.getTokenAllowancesList()) {
            meta.addNonPayerKey(allowance.getOwner(), failureStatus);
        }
        for (final var allowance : op.getNftAllowancesList()) {
            final var ownerId = allowance.getOwner();
            var operatorId =
                    allowance.hasDelegatingSpender() ? allowance.getDelegatingSpender() : ownerId;
            // Since only the owner can grant approveForAll
            if (allowance.getApprovedForAll().getValue()) {
                operatorId = ownerId;
            }
            if (operatorId != ownerId) {
                failureStatus = INVALID_DELEGATING_SPENDER;
            }
            meta.addNonPayerKey(operatorId, failureStatus);
        }
        return meta;
    }

    @Override
    /** {@inheritDoc} */
    public TransactionMetadata preHandleDeleteAllowances(TransactionBody txn) {
        throw new NotImplementedException();
    }

    @Override
    /** {@inheritDoc} */
    public TransactionMetadata preHandleUpdateAccount(TransactionBody txn) {
        throw new NotImplementedException();
    }

    @Override
    /** {@inheritDoc} */
    public TransactionMetadata preHandleCryptoTransfer(TransactionBody txn) {
        throw new NotImplementedException();
    }

    @Override
    /** {@inheritDoc} */
    public TransactionMetadata preHandleAddLiveHash(TransactionBody txn) {
        throw new NotImplementedException();
    }

    @Override
    /** {@inheritDoc} */
    public TransactionMetadata preHandleDeleteLiveHash(TransactionBody txn) {
        throw new NotImplementedException();
    }

    /* --------------- Helper methods --------------- */
    /**
     * Returns metadata for {@code CryptoCreate} transaction needed to validate signatures needed
     * for signing the transaction
     *
     * @param tx given transaction body
     * @param key key provided in the transaction body
     * @param receiverSigReq flag for receiverSigReq on the given transaction body
     * @param payer payer for the transaction
     * @return transaction's metadata needed to validate signatures
     */
    private TransactionMetadata createAccountSigningMetadata(
            final TransactionBody tx,
            final Optional<HederaKey> key,
            final boolean receiverSigReq,
            final AccountID payer) {
        final var meta = new SigTransactionMetadata(accountStore, tx, payer);
        if (receiverSigReq && key.isPresent()) {
            meta.addToReqKeys(key.get());
        }
        return meta;
    }
}
