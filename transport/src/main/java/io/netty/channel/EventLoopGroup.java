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

import io.netty.util.concurrent.EventExecutorGroup;

/**
 * Special {@link EventExecutorGroup} which allows registering {@link Channel}s that get
 * processed for later selection during the event loop.
 *
 */
/**
 * 特殊的 能够注册 在循环中处理后续的select操作 的 {@link Channel}的 EventExecutorGroup
 * {@link EventLoopGroup}是一组 {@link EventLoop}，同时也是一组 {@link java.util.concurrent.Executor}
 * {@link EventLoopGroup}中的核心方法是 {@link #register(Channel)},注册一个 {@link Channel}
 * 其实现就是调用 {@link #next()}获取下一个 {@link EventLoop}注册到该 {@link EventLoop}上
 * {@link EventLoop}也是一种特殊的 {@link EventLoopGroup}，只不过 {@link EventLoop}中group只有它自己一个元素
 */
public interface EventLoopGroup extends EventExecutorGroup {
    /**
     * Return the next {@link EventLoop} to use
     */
    /**
     * 获取下一个要被使用的 {@link EventLoop}
     */
    @Override
    EventLoop next();

    /**
     * Register a {@link Channel} with this {@link EventLoop}. The returned {@link ChannelFuture}
     * will get notified once the registration was complete.
     */
    /**
     * 注册 {@link Channel}到当前的 {@link EventLoop}上 返回一个能在这次注册完成时进行通知的{@link ChannelFuture}
     * @param channel
     * @return
     */
    ChannelFuture register(Channel channel);

    /**
     * Register a {@link Channel} with this {@link EventLoop} using a {@link ChannelFuture}. The passed
     * {@link ChannelFuture} will get notified once the registration was complete and also will get returned.
     */
    ChannelFuture register(ChannelPromise promise);

    /**
     * Register a {@link Channel} with this {@link EventLoop}. The passed {@link ChannelFuture}
     * will get notified once the registration was complete and also will get returned.
     *
     * @deprecated Use {@link #register(ChannelPromise)} instead.
     */
    @Deprecated
    ChannelFuture register(Channel channel, ChannelPromise promise);
}
