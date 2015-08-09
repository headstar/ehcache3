/*
 * Copyright Terracotta, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ehcache.internal.store.heap;

import org.ehcache.internal.TimeSource;
import org.ehcache.internal.serialization.JavaSerializer;
import org.ehcache.spi.cache.Store.ValueHolder;
import org.ehcache.spi.serialization.Serializer;
import org.junit.Test;

import java.io.IOException;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.concurrent.Exchanger;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * @author vfunshteyn
 *
 */
public class ByValueOnHeapValueHolderTest {

  @Test
  public void testValue() {
    String o = "foo";
    ValueHolder<?> vh1 = newValueHolder(o);
    ValueHolder<?> vh2 = newValueHolder(o);
    assertFalse(vh1.value() == vh2.value());
    assertEquals(vh1.value(), vh2.value());
  }

  @Test
  public void testHashCode() {
    ValueHolder<Integer> vh1 = newValueHolder(10);
    ValueHolder<Integer> vh2 = newValueHolder(10);
    // make sure reading the value multiple times doesn't change the hashcode
    vh1.value();
    vh1.value();
    vh2.value();
    vh2.value();
    assertThat(vh1.hashCode(), is(vh2.hashCode()));
  }

  @Test
  public void testEquals() {
    ValueHolder<Integer> vh = newValueHolder(10);
    assertThat(newValueHolder(10), equalTo(vh));
  }

  @Test
  public void testNotEquals() {
    ValueHolder<Integer> vh = newValueHolder(10);
    assertThat(newValueHolder(101), not(equalTo(vh)));
  }

  @Test(expected=NullPointerException.class)
  public void testNullValue() {
    newValueHolder(null);
  }

  @Test
  public void testSerializerGetsDifferentByteBufferOnRead() {
    final Exchanger<ByteBuffer> exchanger = new Exchanger<ByteBuffer>();
    final ReadExchangeSerializer serializer = new ReadExchangeSerializer(exchanger);
    final ByValueOnHeapValueHolder<String> valueHolder = new ByValueOnHeapValueHolder<String>("test it!", System
        .currentTimeMillis(), serializer);

    new Thread(new Runnable() {
      @Override
      public void run() {
        valueHolder.value();
      }
    }).start();

    valueHolder.value();
  }

  private static class ReadExchangeSerializer implements Serializer<String> {

    private final Exchanger<ByteBuffer> exchanger;
    private final Serializer<String> delegate = new JavaSerializer<String>(ByValueOnHeapValueHolderTest.class.getClassLoader());

    private ReadExchangeSerializer(Exchanger<ByteBuffer> exchanger) {
      this.exchanger = exchanger;
    }

    @Override
    public ByteBuffer serialize(String object) throws IOException {
      return delegate.serialize(object);
    }

    @Override
    public String read(ByteBuffer binary) throws IOException, ClassNotFoundException {
      ByteBuffer received = binary;
      ByteBuffer exchanged = null;
      try {
        exchanged = exchanger.exchange(received);
      } catch (InterruptedException e) {
        fail("Received InterruptedException");
      }
      assertNotSame(exchanged, received);
      return delegate.read(received);
    }

    @Override
    public boolean equals(String object, ByteBuffer binary) throws IOException, ClassNotFoundException {
      throw new UnsupportedOperationException("TODO Implement me!");
    }
  }

  private static <V extends Serializable> ValueHolder<V> newValueHolder(V value) {
    return new ByValueOnHeapValueHolder<V>(value, TestTimeSource.INSTANCE.getTimeMillis(), new JavaSerializer<V>(ByValueOnHeapValueHolderTest.class.getClassLoader()));
  }

  private static class TestTimeSource implements TimeSource {

    static final TestTimeSource INSTANCE = new TestTimeSource();

    private long time = 1;

    @Override
    public long getTimeMillis() {
      return time;
    }

    private void advanceTime(long delta) {
      this.time += delta;
    }
  }
}
