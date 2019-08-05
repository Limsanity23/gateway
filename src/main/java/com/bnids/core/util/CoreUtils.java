/*
 * BNIndustry Inc., Software License, Version 1.0
 *
 * Copyright (c) 2018 BNIndustry Inc.,
 * All rights reserved.
 *
 *  DON'T COPY OR REDISTRIBUTE THIS SOURCE CODE WITHOUT PERMISSION.
 *  THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 *  WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 *  OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL <<BNIndustry Inc.>> OR ITS
 *  CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 *  USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 *  ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 *  OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 *  OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 *  SUCH DAMAGE.
 *
 *  For more information on this product, please see www.bnids.com
 */

package com.bnids.core.util;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author yannishin
 */
@Component
public class CoreUtils {
    private static String distSystemYn;
    private static int distSystemNo;

    private static final long HOST_ID = getHostId();
    private static long lastTime = 0;
    private static long clockSequence = 0;

    private final static AtomicInteger counter = new AtomicInteger();


    /**
     * 분산환경시스템 여부
     *
     * @param distSystemYn Y:분산, N:단독서버
     */
    //@Value("${distributed.system.yn}")
    public void setDistSystemYn(String distSystemYn) {
        CoreUtils.distSystemYn = distSystemYn;
    }

    /**
     * 분산환경시스템 호기정보
     *
     * @param distSystemNo 호기정보 (0 ~ 999)
     */
    //@Value("${distributed.system.no}")
    public void setDistSystemNo(int distSystemNo) {
        CoreUtils.distSystemNo = distSystemNo;
    }

    /**
     * prefix 문자열을 붙인 고유키를 반환합니다.
     * <p>prefix 문자는 가급적 1Byte로 사용하기 바랍니다.<br/>
     * 최종 생성되는 고유키가 30Bytes 이내인지 확인하시기 바랍니다.<br/></p>
     *
     * @param prefix prefix 문자열
     * @return 고유키
     */
    public static String generateUniqueId(String prefix) {
        return prefix + generateUniqueId();
    }

    /**
     * 시간적인 시퀀스를 보장할 수 있는 고유키를 반환합니다.
     * <p>분산시스템 환경 설정에 따라 15Bytes 또는 18Bytes 키를 반환합니다.</p>
     * <pre>
     * application.properties 파일
     *     distributed.system.yn=N : 15Bytes ID
     *     distributed.system.yn=Y : 18Bytes ID
     *         (15Bytes ID + 호기정보 3Bytes)
     * </pre>
     *
     * @return 고유키
     */
    public static String generateUniqueId() {
        int atomicInt = counter.incrementAndGet();

        UUID uuid = generateIdFromTimestamp(System.currentTimeMillis());
        String id = uuid.toString();
        String[] idArr = id.split("-");

        int lastSeq = Integer.parseInt(idArr[2], 16) + Integer.parseInt(idArr[3], 16);
        String seqStr = Integer.toHexString(lastSeq);
        seqStr = seqStr.substring(seqStr.length() - 3);

        id = idArr[0] + idArr[1] + StringUtils.leftPad(seqStr, 3, "0") + StringUtils.leftPad("" + atomicInt, 2, "0")
                + ("Y".equals(distSystemYn) ? StringUtils.leftPad("" + distSystemNo, 1, "0") : "");

        return id;
    }

    /**
     * 시간적인 시퀀스를 보장할 수 있는 UUID를 반환합니다.
     *
     * @return UUID
     */
    public static final UUID generateUUID() {
        return generateIdFromTimestamp(System.currentTimeMillis());
    }

    private synchronized static final UUID generateIdFromTimestamp(long currentTimeMillis) {
        long time = 0;

        //synchronized (IdGenerator.class) {
        if (currentTimeMillis > lastTime) {
            lastTime = currentTimeMillis;
            clockSequence = 0;
            counter.set(0);
        } else {
            ++clockSequence;
        }
        //}

        // low Time
        time = currentTimeMillis << 32;

        // mid Time
        time |= ((currentTimeMillis & 0xFFFF00000000L) >> 16);

        // hi Time
        time |= 0x1000 | ((currentTimeMillis >> 48) & 0x0FFF);

        long lsb = (HOST_ID >>> 48) + clockSequence;
        lsb = (lsb << 48) | (HOST_ID & 0x0000FFFFFFFFFFFFL);

        return new UUID(time, lsb);
    }

    private static final long getHostId() {
        long macAddressAsLong = 0;

        try {
            Random random = new Random();
            InetAddress address = InetAddress.getLocalHost();

            Enumeration<NetworkInterface> enis = NetworkInterface.getNetworkInterfaces();
            while (enis.hasMoreElements()) {
                NetworkInterface sni = (NetworkInterface) enis.nextElement();

                Enumeration<InetAddress> eia = sni.getInetAddresses();
                while (eia.hasMoreElements()) {
                    InetAddress sia = (InetAddress) eia.nextElement();
                    if (!sia.isLoopbackAddress() && !sia.isLinkLocalAddress() && !sia.isSiteLocalAddress()) {
                        address = sia;
                    }
                }
            }
            NetworkInterface ni = NetworkInterface.getByInetAddress(address);

            if (ni != null) {
                byte[] mac = ni.getHardwareAddress();
                if (null != mac) {
                    random.nextBytes(mac); // we don't really want to reveal the actual MAC address

                    // Converts array of unsigned bytes to an long
                    if (mac != null) {
                        for (int i = 0; i < mac.length; i++) {
                            macAddressAsLong <<= 8;
                            macAddressAsLong ^= (long) mac[i] & 0xFF;
                        }
                    }
                } else {
                    macAddressAsLong = random.nextLong();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return macAddressAsLong;
    }

    /**
     *
     * @param type
     * @param cnt
     * @return
     */
    public static String randomValue(String type, int cnt) {

        StringBuffer strPwd = new StringBuffer();
        char str[] = new char[1];
        // 특수기호 포함
        if (type.equals("P")) {
            for (int i = 0; i < cnt; i++) {
                str[0] = (char) ((Math.random() * 94) + 33);
                strPwd.append(str);
            }
            // 대문자로만
        } else if (type.equals("A")) {
            for (int i = 0; i < cnt; i++) {
                str[0] = (char) ((Math.random() * 26) + 65);
                strPwd.append(str);
            }
            // 소문자로만
        } else if (type.equals("S")) {
            for (int i = 0; i < cnt; i++) {
                str[0] = (char) ((Math.random() * 26) + 97);
                strPwd.append(str);
            }
            // 숫자형으로
        } else if (type.equals("I")) {
            int strs[] = new int[1];
            for (int i = 0; i < cnt; i++) {
                strs[0] = (int) (Math.random() * 9);
                strPwd.append(strs[0]);
            }
            // 소문자, 숫자형
        } else if (type.equals("C")) {
            Random rnd = new Random();
            for (int i = 0; i < cnt; i++) {
                if (rnd.nextBoolean()) {
                    strPwd.append((char) ((int) (rnd.nextInt(26)) + 97));
                } else {
                    strPwd.append((rnd.nextInt(10)));
                }
            }
        }
        return strPwd.toString();
    }
}
