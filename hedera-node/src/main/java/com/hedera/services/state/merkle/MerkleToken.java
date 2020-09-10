package com.hedera.services.state.merkle;

/*-
 * ‌
 * Hedera Services Node
 * ​
 * Copyright (C) 2018 - 2020 Hedera Hashgraph, LLC
 * ​
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
 * ‍
 */

import com.google.common.base.MoreObjects;
import com.hedera.services.legacy.core.jproto.JKey;
import com.hedera.services.state.serdes.DomainSerdes;
import com.hedera.services.state.submerkle.EntityId;
import com.swirlds.common.FCMValue;
import com.swirlds.common.FastCopyable;
import com.swirlds.common.io.SerializableDataInputStream;
import com.swirlds.common.io.SerializableDataOutputStream;
import com.swirlds.common.io.SerializedObjectProvider;
import com.swirlds.common.merkle.MerkleLeaf;
import com.swirlds.common.merkle.utility.AbstractMerkleNode;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.Objects;
import java.util.Optional;

import static com.hedera.services.legacy.core.jproto.JKey.equalUpToDecodability;
import static com.hedera.services.utils.MiscUtils.describe;

public class MerkleToken extends AbstractMerkleNode implements FCMValue, MerkleLeaf  {
	static final int MAX_CONCEIVABLE_SYMBOL_LENGTH = 256;
	static final int MERKLE_VERSION = 1;
	static final long RUNTIME_CONSTRUCTABLE_ID = 0xd23ce8814b35fc2fL;
	static DomainSerdes serdes = new DomainSerdes();

	public static final JKey UNUSED_KEY = null;

	@Deprecated
	public static final MerkleToken.Provider LEGACY_PROVIDER = new MerkleToken.Provider();

	private int divisibility;
	private long tokenFloat;
	private JKey adminKey;
	private JKey kycKey = UNUSED_KEY;
	private JKey wipeKey = UNUSED_KEY;
	private JKey supplyKey = UNUSED_KEY;
	private JKey freezeKey = UNUSED_KEY;
	private String symbol;
	private boolean deleted;
	private boolean accountsFrozenByDefault;
	private boolean accountKycGrantedByDefault;
	private EntityId treasury;

	@Deprecated
	public static class Provider implements SerializedObjectProvider {
		@Override
		public FastCopyable deserialize(DataInputStream _in) throws IOException {
			throw new UnsupportedOperationException();
		}
	}

	public MerkleToken() { }

	public MerkleToken(
			long tokenFloat,
			int divisibility,
			JKey adminKey,
			String symbol,
			boolean accountsFrozenByDefault,
			boolean accountKycGrantedByDefault,
			EntityId treasury
	) {
		this.tokenFloat = tokenFloat;
		this.divisibility = divisibility;
		this.adminKey = adminKey;
		this.symbol = symbol;
		this.accountsFrozenByDefault = accountsFrozenByDefault;
		this.accountKycGrantedByDefault = accountKycGrantedByDefault;
		this.treasury = treasury;
	}

	/* Object */
	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || MerkleToken.class != o.getClass()) {
			return false;
		}

		var that = (MerkleToken)o;
		return this.deleted == that.deleted &&
				this.tokenFloat == that.tokenFloat &&
				this.divisibility == that.divisibility &&
				this.accountsFrozenByDefault == that.accountsFrozenByDefault &&
				this.accountKycGrantedByDefault == that.accountKycGrantedByDefault &&
				Objects.equals(this.symbol, that.symbol) &&
				Objects.equals(this.treasury, that.treasury) &&
				equalUpToDecodability(this.wipeKey, that.wipeKey) &&
				equalUpToDecodability(this.supplyKey, that.supplyKey) &&
				equalUpToDecodability(this.adminKey, that.adminKey) &&
				equalUpToDecodability(this.freezeKey, that.freezeKey) &&
				equalUpToDecodability(this.kycKey, that.kycKey);
	}

	@Override
	public int hashCode() {
		return Objects.hash(
				deleted,
				tokenFloat,
				divisibility,
				adminKey,
				freezeKey,
				kycKey,
				wipeKey,
				supplyKey,
				symbol,
				accountsFrozenByDefault,
				accountKycGrantedByDefault,
				treasury);
	}

	/* --- Bean --- */
	@Override
	public String toString() {
		return MoreObjects.toStringHelper(MerkleToken.class)
				.add("deleted", deleted)
				.add("symbol", symbol)
				.add("treasury", treasury.toAbbrevString())
				.add("float", tokenFloat)
				.add("divisibility", divisibility)
				.add("adminKey", describe(adminKey))
				.add("kycKey", describe(kycKey))
				.add("wipeKey", describe(wipeKey))
				.add("supplyKey", describe(supplyKey))
				.add("freezeKey", describe(freezeKey))
				.add("accountKycGrantedByDefault", accountKycGrantedByDefault)
				.add("accountsFrozenByDefault", accountsFrozenByDefault)
				.toString();
	}

	/* --- MerkleLeaf --- */
	@Override
	public long getClassId() {
		return RUNTIME_CONSTRUCTABLE_ID;
	}

	@Override
	public int getVersion() {
		return MERKLE_VERSION;
	}

	@Override
	public void deserialize(SerializableDataInputStream in, int version) throws IOException {
		deleted = in .readBoolean();
		adminKey = serdes.deserializeKey(in);
		symbol = in.readNormalisedString(MAX_CONCEIVABLE_SYMBOL_LENGTH);
		treasury = in.readSerializable();
		tokenFloat = in.readLong();
		divisibility = in.readInt();
		accountsFrozenByDefault = in.readBoolean();
		accountKycGrantedByDefault = in.readBoolean();
		freezeKey = serdes.readNullable(in, serdes::deserializeKey);
		kycKey = serdes.readNullable(in, serdes::deserializeKey);
		supplyKey = serdes.readNullable(in, serdes::deserializeKey);
		wipeKey = serdes.readNullable(in, serdes::deserializeKey);
	}

	@Override
	public void serialize(SerializableDataOutputStream out) throws IOException {
		out.writeBoolean(deleted);
		serdes.serializeKey(adminKey, out);
		out.writeNormalisedString(symbol);
		out.writeSerializable(treasury, true);
		out.writeLong(tokenFloat);
		out.writeInt(divisibility);
		out.writeBoolean(accountsFrozenByDefault);
		out.writeBoolean(accountKycGrantedByDefault);
		serdes.writeNullable(freezeKey, out, serdes::serializeKey);
		serdes.writeNullable(kycKey, out, serdes::serializeKey);
		serdes.writeNullable(supplyKey, out, serdes::serializeKey);
		serdes.writeNullable(wipeKey, out, serdes::serializeKey);
	}

	/* --- FastCopyable --- */
	@Override
	public MerkleToken copy() {
		var fc = new MerkleToken(
			tokenFloat,
			divisibility,
			adminKey,
			symbol,
			accountsFrozenByDefault,
			accountKycGrantedByDefault,
			treasury);
		fc.setDeleted(deleted);
		if (freezeKey != UNUSED_KEY) {
			fc.setFreezeKey(freezeKey);
		}
		if (kycKey != UNUSED_KEY) {
			fc.setKycKey(kycKey);
		}
		if (wipeKey != UNUSED_KEY) {
			fc.setWipeKey(wipeKey);
		}
		if (supplyKey != UNUSED_KEY) {
			fc.setSupplyKey(supplyKey);
		}
		return fc;
	}

	@Override
	public void delete() {
	}

	/* --- Bean --- */
	public long tokenFloat() {
		return tokenFloat;
	}

	public int divisibility() {
		return divisibility;
	}

	public JKey adminKey() {
		return adminKey;
	}

	public Optional<JKey> freezeKey() {
		return Optional.ofNullable(freezeKey);
	}

	public boolean hasFreezeKey() {
		return freezeKey != UNUSED_KEY;
	}

	public Optional<JKey> kycKey() {
		return Optional.ofNullable(kycKey);
	}

	public boolean hasKycKey() {
		return kycKey != UNUSED_KEY;
	}

	public void setFreezeKey(JKey freezeKey) {
		this.freezeKey = freezeKey;
	}

	public void setKycKey(JKey kycKey) {
		this.kycKey = kycKey;
	}

	public Optional<JKey> supplyKey() {
		return Optional.ofNullable(supplyKey);
	}

	public boolean hasSupplyKey() {
		return supplyKey != UNUSED_KEY;
	}

	public void setSupplyKey(JKey supplyKey) {
		this.supplyKey = supplyKey;
	}

	public Optional<JKey> wipeKey() {
		return Optional.ofNullable(wipeKey);
	}

	public boolean hasWipeKey() {
		return wipeKey != UNUSED_KEY;
	}

	public void setWipeKey(JKey wipeKey) {
		this.wipeKey = wipeKey;
	}

	public boolean isDeleted() {
		return deleted;
	}

	public void setDeleted(boolean deleted) {
		this.deleted = deleted;
	}

	public String symbol() {
		return symbol;
	}

	public void setSymbol(String symbol) {
		this.symbol = symbol;
	}

	public void setTreasury(EntityId treasury) {
		this.treasury = treasury;
	}

	public void setAdminKey(JKey adminKey) {
		this.adminKey = adminKey;
	}

	public boolean accountsAreFrozenByDefault() {
		return accountsFrozenByDefault;
	}

	public boolean accountKycGrantedByDefault() {
		return accountKycGrantedByDefault;
	}

	public EntityId treasury() {
		return treasury;
	}

	public void adjustFloatBy(long amount) {
		var newFloat = tokenFloat + amount;
		if (newFloat < 0) {
			throw new IllegalArgumentException(String.format("Cannot set token float to %d!", newFloat));
		}
		tokenFloat += amount;
	}

	void setAccountsFrozenByDefault(boolean accountsFrozenByDefault) {
		this.accountsFrozenByDefault = accountsFrozenByDefault;
	}

	void setAccountKycGrantedByDefault(boolean accountKycGrantedByDefault) {
		this.accountKycGrantedByDefault = accountKycGrantedByDefault;
	}
}