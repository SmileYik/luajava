/*
 * FieldEntity.java, SmileYik, 2025-8-10
 * Copyright (c) 2025 Smile Yik
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.eu.smileyik.luajava.javaapi;

public class FieldEntity {
    private byte a = 0;
    private short b = 1;
    private int c = 2;
    private long d = 3;
    private float e = 4;
    private double f = 5;
    private char g = '6';
    private String h = "7";
    private boolean i = false;
    private AField j = new AField();

    public byte pa = 0;
    public short pb = 1;
    public int pc = 2;
    public long pd = 3;
    public float pe = 4;
    public double pf = 5;
    public char pg = '6';
    public String ph = "7";
    public boolean pi = true;
    public AField pj = new AField();

    public static byte psa = 0;
    public static short psb = 1;
    public static int psc = 2;
    public static long psd = 3;
    public static float pse = 4;
    public static double psf = 5;
    public static char psg = '6';
    public static String psh = "7";
    public static boolean psi = false;
    public static AField psj = new AField();

    private static byte sa = 0;
    private static short sb = 1;
    private static int sc = 2;
    private static long sd = 3;
    private static float se = 4;
    private static double sf = 5;
    private static char sg = '6';
    private static String sh = "7";
    private static boolean si = false;
    private static AField sj = new AField();

    public static final byte fsa = 8;
    public static final short fsb = 9;
    public static final int fsc = 10;
    public static final long fsd = 11L;
    public static final float fse = 12.0f;
    public static final double fsf = 13.0;
    public static final char fsg = 'A';
    public static final String fsh = "B";
    public static final boolean fsi = true;
    public static final AField fsj = new AField();

    public static class AField {
        private String k = "k";
    }

    @Override
    public String toString() {
        return "FieldEntity{" +
                "a=" + a +
                ", b=" + b +
                ", c=" + c +
                ", d=" + d +
                ", e=" + e +
                ", f=" + f +
                ", g=" + g +
                ", h='" + h + '\'' +
                ", i=" + i +
                ", j=" + j +
                ", pa=" + pa +
                ", pb=" + pb +
                ", pc=" + pc +
                ", pd=" + pd +
                ", pe=" + pe +
                ", pf=" + pf +
                ", pg=" + pg +
                ", ph='" + ph + '\'' +
                ", pi=" + pi +
                ", pj=" + pj +
                '}';
    }
}
