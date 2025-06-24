/*
 * $Id: LuaStateFactory.java,v 1.4 2006-12-22 14:06:40 thiago Exp $
 * Copyright (C) 2003-2007 Kepler Project.
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 * CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package org.keplerproject.luajava;

import org.eu.smileyik.luajava.util.ParamRef;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This class is responsible for instantiating new LuaStates.
 * When a new LuaState is instantiated it is put into a List
 * and an index is returned. This index is registred in Lua
 * and it is used to find the right LuaState when lua calls
 * a Java Function.
 *
 * @author Thiago Ponte
 */
public final class LuaStateFactory {
    /**
     * state id generator
     */
    private static final AtomicInteger COUNTER = new AtomicInteger(0);
    /**
     * all lua state instances
     */
    private static final ConcurrentMap<Integer, LuaStateFacade> STATES = new ConcurrentHashMap<>();
    private static final ConcurrentMap<Long, Integer> CPER_TP_STATE_ID_MAP = new ConcurrentHashMap<>();

    /**
     * Non-public constructor.
     */
    private LuaStateFactory() {
    }

    /**
     * Method that creates a new instance of LuaState
     *
     * @return LuaState
     */
    public synchronized static LuaStateFacade newLuaState() {
        return newLuaState(true);
    }

    /**
     * Method that creates a new instance of LuaState
     *
     * @return LuaState
     */
    public synchronized static LuaStateFacade newLuaState(boolean ignoreNotPublic) {
        LuaStateFacade facade = new LuaStateFacade(COUNTER.getAndIncrement(), ignoreNotPublic);
        STATES.put(facade.getStateId(), facade);
        CPER_TP_STATE_ID_MAP.put(facade.getCPtrPeer(), facade.getStateId());
        return facade;
    }

    /**
     * Returns a existing instance of LuaState
     *
     * @param index
     * @return LuaState
     */
    public static LuaStateFacade getExistingState(int index) {
        return STATES.get(index);
    }

    /**
     * Receives a existing LuaState and checks if it exists in the states list.
     * If it doesn't exist adds it to the list.
     *
     * @param L              lua state
     * @param cPtr           cPtr overwrite
     * @param existLuaState  will set luaStateFacade if cPtr exist
     * @return int
     */
    public synchronized static int insertLuaState(LuaStateFacade L, CPtr cPtr, ParamRef<LuaStateFacade> existLuaState) {
        long target = cPtr == null ? L.getCPtrPeer() : cPtr.getPeer();
        Integer stateId = CPER_TP_STATE_ID_MAP.get(target);
        if (stateId == null) {
            stateId = COUNTER.incrementAndGet();
            CPER_TP_STATE_ID_MAP.put(target, stateId);
            STATES.put(stateId, L);
        } else {
            existLuaState.setParam(STATES.get(stateId));
        }
        return stateId;
    }

    /**
     * removes the luaState from the states list
     * this method normally called when close LuaStateFacade.
     *
     * @param idx
     */
    public static void removeLuaState(int idx) {
        LuaStateFacade remove = STATES.remove(idx);
        if (remove != null) {
            long cPtrPeer = remove.getCPtrPeer();
            CPER_TP_STATE_ID_MAP.remove(cPtrPeer, idx);
        }
    }
}
