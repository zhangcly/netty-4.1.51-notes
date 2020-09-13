/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package io.netty.channel;

import io.netty.util.concurrent.OrderedEventExecutor;

/**
 * Will handle all the I/O operations for a {@link Channel} once registered.
 *
 * One {@link EventLoop} instance will usually handle more than one {@link Channel} but this may depend on
 * implementation details and internals.
 *
 */
/**
 * <p>
 * EventLoop接口继承了ExecutorService接口，表明这也是个线程池。
 * <p/>
 * <p>
 * 当一个Channel注册完成之后，EventLoop就会处理这个channel的所有I/O操作。
 * 通常来说，一个EventLoop会处理多个Channel，但也取决于实现的细节和内部。(上面注释的翻译)
 * <p/>
 * EventLoop接口继承了 {@link EventLoopGroup}和 {@link OrderedEventExecutor}两个接口，
 * {@link OrderedEventExecutor}接口表明这个类会有序地执行被提交的任务。
 * TODO {@link EventLoopGroup}的特性
 */
public interface EventLoop extends OrderedEventExecutor, EventLoopGroup {
    @Override
    EventLoopGroup parent();
}
