/*
 * Copyright (C) 2020-2022 Hedera Hashgraph, LLC
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
package com.hedera.node.app.service.mono.fees.calculation.token.txns;

import static com.hedera.node.app.hapi.fees.usage.SingletonEstimatorUtils.ESTIMATOR_UTILS;
import static com.hedera.node.app.service.mono.fees.calculation.token.queries.GetTokenInfoResourceUsage.ifPresent;

import com.hedera.node.app.hapi.fees.usage.EstimatorFactory;
import com.hedera.node.app.hapi.fees.usage.SigUsage;
import com.hedera.node.app.hapi.fees.usage.TxnUsageEstimator;
import com.hedera.node.app.hapi.fees.usage.token.TokenUpdateUsage;
import com.hedera.node.app.hapi.utils.exception.InvalidTxBodyException;
import com.hedera.node.app.hapi.utils.fee.SigValueObj;
import com.hedera.node.app.service.mono.context.primitives.StateView;
import com.hedera.node.app.service.mono.fees.calculation.TxnResourceUsageEstimator;
import com.hederahashgraph.api.proto.java.FeeData;
import com.hederahashgraph.api.proto.java.TokenInfo;
import com.hederahashgraph.api.proto.java.TransactionBody;
import java.util.function.BiFunction;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class TokenUpdateResourceUsage extends AbstractTokenResourceUsage
        implements TxnResourceUsageEstimator {
    private static final BiFunction<TransactionBody, TxnUsageEstimator, TokenUpdateUsage> factory =
            TokenUpdateUsage::newEstimate;

    @Inject
    public TokenUpdateResourceUsage(final EstimatorFactory estimatorFactory) {
        super(estimatorFactory);
    }

    @Override
    public boolean applicableTo(final TransactionBody txn) {
        return txn.hasTokenUpdate();
    }

    @Override
    public FeeData usageGiven(
            final TransactionBody txn, final SigValueObj svo, final StateView view)
            throws InvalidTxBodyException {
        final var op = txn.getTokenUpdate();
        final var sigUsage =
                new SigUsage(
                        svo.getTotalSigCount(), svo.getSignatureSize(), svo.getPayerAcctSigCount());
        final var optionalInfo = view.infoForToken(op.getToken());
        if (optionalInfo.isPresent()) {
            final var info = optionalInfo.get();
            final var estimate =
                    factory.apply(txn, estimatorFactory.get(sigUsage, txn, ESTIMATOR_UTILS))
                            .givenCurrentExpiry(info.getExpiry().getSeconds())
                            .givenCurrentAdminKey(
                                    ifPresent(info, TokenInfo::hasAdminKey, TokenInfo::getAdminKey))
                            .givenCurrentFreezeKey(
                                    ifPresent(
                                            info, TokenInfo::hasFreezeKey, TokenInfo::getFreezeKey))
                            .givenCurrentWipeKey(
                                    ifPresent(info, TokenInfo::hasWipeKey, TokenInfo::getWipeKey))
                            .givenCurrentSupplyKey(
                                    ifPresent(
                                            info, TokenInfo::hasSupplyKey, TokenInfo::getSupplyKey))
                            .givenCurrentKycKey(
                                    ifPresent(info, TokenInfo::hasKycKey, TokenInfo::getKycKey))
                            .givenCurrentFeeScheduleKey(
                                    ifPresent(
                                            info,
                                            TokenInfo::hasFeeScheduleKey,
                                            TokenInfo::getFeeScheduleKey))
                            .givenCurrentPauseKey(
                                    ifPresent(info, TokenInfo::hasPauseKey, TokenInfo::getPauseKey))
                            .givenCurrentMemo(info.getMemo())
                            .givenCurrentName(info.getName())
                            .givenCurrentSymbol(info.getSymbol());
            if (info.hasAutoRenewAccount()) {
                estimate.givenCurrentlyUsingAutoRenewAccount();
            }
            return estimate.get();
        } else {
            return FeeData.getDefaultInstance();
        }
    }
}
