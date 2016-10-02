package lexek.wschat.services.geoip;

import org.jvnet.hk2.annotations.Contract;

import java.net.InetAddress;

@Contract
public interface GeoIpService {
    String getLocation(InetAddress ip) throws Exception;
}
