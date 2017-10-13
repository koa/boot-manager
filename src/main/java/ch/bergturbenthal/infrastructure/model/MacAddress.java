package ch.bergturbenthal.infrastructure.model;

import java.util.regex.Pattern;

import lombok.Value;

@Value
public class MacAddress implements Comparable<MacAddress> {
    private static Pattern REMOVE_MATCHER = Pattern.compile("[^0-9a-f]");

    public static MacAddress parseAddress(final String address) {
        final String cleanAddress = REMOVE_MATCHER.matcher(address.toLowerCase()).replaceAll("");
        if (cleanAddress.length() != 12) {
            throw new IllegalArgumentException("Invalid mac address " + address);
        }
        return new MacAddress(Long.parseUnsignedLong(cleanAddress, 16));

    }

    private long address;

    @Override
    public int compareTo(final MacAddress o) {
        return Long.compare(address, o.address);
    }

    @Override
    public String toString() {
        final String hexString = Long.toHexString(address);
        CharSequence filledHexString;
        if (hexString.length() < 12) {
            final StringBuilder sb = new StringBuilder();
            for (int i = hexString.length(); i < 12; i++) {
                sb.append('0');
            }
            sb.append(hexString);
            filledHexString = sb;
        } else {
            filledHexString = hexString;
        }
        final StringBuilder result = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            if (i > 0) {
                result.append(':');
            }
            result.append(filledHexString.subSequence(i * 2, i * 2 + 2));
        }
        return result.toString();

    }
}
