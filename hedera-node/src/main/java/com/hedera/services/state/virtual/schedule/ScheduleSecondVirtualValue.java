package com.hedera.services.state.virtual.schedule;

/*-
 * ‌
 * Hedera Services Node
 * ​
 * Copyright (C) 2018 - 2022 Hedera Hashgraph, LLC
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

import java.beans.Transient;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.Supplier;

import com.google.common.base.MoreObjects;
import com.hedera.services.state.submerkle.RichInstant;
import com.swirlds.common.io.streams.SerializableDataInputStream;
import com.swirlds.common.io.streams.SerializableDataOutputStream;
import com.swirlds.virtualmap.VirtualValue;

import org.eclipse.collections.api.list.primitive.ImmutableLongList;
import org.eclipse.collections.api.list.primitive.LongList;
import org.eclipse.collections.api.list.primitive.MutableLongList;
import org.eclipse.collections.impl.list.mutable.primitive.LongArrayList;

public class ScheduleSecondVirtualValue implements VirtualValue {

	static final int CURRENT_VERSION = 1;

	static final long RUNTIME_CONSTRUCTABLE_ID = 0x1d2377926e3a85fcL;

	/** The value must be a list because more than one schedule can be scheduled for the same instant. */
	private final NavigableMap<RichInstant, ImmutableLongList> ids;

	private boolean immutable;


	public ScheduleSecondVirtualValue() {
		this(TreeMap::new);
	}

	public ScheduleSecondVirtualValue(Map<RichInstant, ? extends LongList> ids) {
		this();
		ids.forEach((k, v) -> this.ids.put(k, v.toImmutable()));
	}

	private ScheduleSecondVirtualValue(Supplier<NavigableMap<RichInstant, ImmutableLongList>> ids) {
		this.ids = ids.get();
	}


	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || ScheduleSecondVirtualValue.class != o.getClass()) {
			return false;
		}

		var that = (ScheduleSecondVirtualValue) o;
		return Objects.equals(this.ids, that.ids);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(ids);
	}

	@Override
	public String toString() {
		var helper = MoreObjects.toStringHelper(ScheduleSecondVirtualValue.class)
				.add("ids", ids);
		return helper.toString();
	}

	@Override
	public void deserialize(SerializableDataInputStream in, int version) throws IOException {
		int s = in.readInt();
		ids.clear();
		for (int x = 0; x <	s; ++x) {
			int n = in.readInt();
			LongArrayList l = new LongArrayList(n);
			for (int y = 0; y <	n; ++y) {
				l.add(in.readLong());
			}
			if (l.size() > 0) {
				ids.put(RichInstant.from(in), l.toImmutable());
			}
		}
	}

	@Override
	public void deserialize(ByteBuffer in, int version) throws IOException {
		int s = in.getInt();
		ids.clear();
		for (int x = 0; x <	s; ++x) {
			int n = in.getInt();
			LongArrayList l = new LongArrayList(n);
			for (int y = 0; y <	n; ++y) {
				l.add(in.getLong());
			}
			if (l.size() > 0) {
				ids.put(new RichInstant(in.getLong(), in.getInt()), l.toImmutable());
			}
		}
	}

	@Override
	public void serialize(SerializableDataOutputStream out) throws IOException {
		out.writeInt(ids.size());
		for (var e : ids.entrySet()) {
			out.writeInt(e.getValue().size());
			for (int x = 0; x < e.getValue().size(); ++x) {
				out.writeLong(e.getValue().get(x));
			}
			e.getKey().serialize(out);
		}
	}

	@Override
	public void serialize(ByteBuffer out) throws IOException {
		out.putInt(ids.size());
		for (var e : ids.entrySet()) {
			out.putInt(e.getValue().size());
			for (int x = 0; x < e.getValue().size(); ++x) {
				out.putLong(e.getValue().get(x));
			}
			out.putLong(e.getKey().getSeconds());
			out.putInt(e.getKey().getNanos());
		}
	}

	@Override
	public long getClassId() {
		return RUNTIME_CONSTRUCTABLE_ID;
	}

	@Override
	public int getVersion() {
		return CURRENT_VERSION;
	}

	@Override
	public ScheduleSecondVirtualValue copy() {
		var fc = new ScheduleSecondVirtualValue(ids);

		this.setImmutable(true);

		return fc;
	}

	public void add(RichInstant instant, LongList idList) {
		throwIfImmutable("Cannot add to ids if it's immutable.");

		final ImmutableLongList l = ids.get(instant);

		final MutableLongList m;
		if (l == null) {
			m = new LongArrayList(idList.size());
		} else {
			m = new LongArrayList(l.size() + idList.size());
			l.forEach(m::add);
		}
		m.addAll(idList);
		ids.put(instant, m.toImmutable());
	}

	public void removeId(RichInstant instant, long id) {
		throwIfImmutable("Cannot remove from ids if it's immutable.");

		final ImmutableLongList curList = ids.get(instant);

		if (curList != null) {
			final var newList = new LongArrayList(curList.size());
			curList.forEach(l -> {
				if (l != id) {
					newList.add(l);
				}
			});
			if (newList.size() > 0) {
				ids.put(instant, newList.toImmutable());
			} else {
				ids.remove(instant);
			}
		}
	}

	public NavigableMap<RichInstant, ImmutableLongList> getIds() {
		return Collections.unmodifiableNavigableMap(ids);
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	public final boolean isImmutable() {
		return immutable;
	}

	@Transient
	protected final void setImmutable(final boolean immutable) {
		this.immutable = immutable;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void release() {
		// nothing to release
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ScheduleSecondVirtualValue asReadOnly() {
		var c = new ScheduleSecondVirtualValue(this::getIds);
		c.setImmutable(true);
		return c;
	}

	/**
	 * Needed until getForModify works on VirtualMap
	 * @return a copy of this without marking this as immutable
	 */
	public ScheduleSecondVirtualValue asWritable() {
		return new ScheduleSecondVirtualValue(this.ids);
	}
}