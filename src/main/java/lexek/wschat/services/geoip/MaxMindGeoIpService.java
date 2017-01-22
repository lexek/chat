package lexek.wschat.services.geoip;

import com.maxmind.db.CHMCache;
import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.model.CityResponse;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;

@Service
public class MaxMindGeoIpService implements GeoIpService {
    private final DatabaseReader databaseReader;

    @Inject
    public MaxMindGeoIpService() throws IOException {
        this.databaseReader = new DatabaseReader
            .Builder(new File("geoip.mmdb"))
            .withCache(new CHMCache())
            .build();
    }

    @Override
    public String getLocation(InetAddress ip) throws Exception {
        CityResponse response = databaseReader.city(ip);
        String country = response.getCountry().getName();
        if (country == null) {
            country = "Unknown";
        }
        String city = response.getCity().getName();
        if (city == null) {
            city = "Unknown";
        }
        return country + " - " + city;
    }
}
