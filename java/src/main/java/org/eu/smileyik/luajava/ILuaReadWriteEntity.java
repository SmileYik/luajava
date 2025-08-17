package org.eu.smileyik.luajava;

import java.nio.ByteBuffer;

public interface ILuaReadWriteEntity {

    public int setBufferSize(int bufferSize);
    /**
     * control C side alloc how many memory
     */
    public int bufferSize();

    /**
     * C side set latest allocated memory ptr
     */
    public void setDataPtr(long ptr);
    public long getDataPtr();

    /**
     * dump from lua
     * @param facade lua state
     * @param in c allocate bytebuffer, read bytes from here.
     */
    public void luaWrite(LuaStateFacade facade, ByteBuffer in);

    /**
     * load to lua
     * @param facade lua state
     * @param out c allocate bytebuffer, write bytes to here.
     * @return written bytes
     */
    public int luaRead(LuaStateFacade facade, ByteBuffer out);
}
