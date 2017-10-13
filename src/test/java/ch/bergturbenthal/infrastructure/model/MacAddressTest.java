package ch.bergturbenthal.infrastructure.model;

import org.junit.Assert;
import org.junit.Test;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MacAddressTest {

    @Test
    public void testFormatMacAddress() {
        Assert.assertEquals("00:00:00:00:00:00", new MacAddress(0).toString());
    }

    @Test
    public void testParseMacAddress() {
        final MacAddress macAddress = MacAddress.parseAddress("07-4B-02-7C-5E-F5");
        log.info("Decimal  : " + macAddress.getAddress());
        log.info("Formatted: " + macAddress);
    }
}
