import inet.ipaddr.IPAddress;
import inet.ipaddr.IPAddressString;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;

public class Main {
    private static final int MERGE_TO_CIDR_AMOUNT_IPV4 = 5;
    private static final int MERGE_TO_CIDR_AMOUNT_IPV6 = 5;
    private static final int IPV6_PREFIX_LENGTH = 56;
    private static final int IPV4_PREFIX_LENGTH = 24;

    public static void main(String[] args) throws IOException {
        File file = new File(args[0]);
        if (!file.exists()) {
            throw new IllegalArgumentException("The file " + args[0] + " does not exist");
        }
        Set<String> sorted = Files.readAllLines(file.toPath()).stream().sorted().collect(Collectors.toCollection(LinkedHashSet::new));
        Set<String> createdCIDR = new LinkedHashSet<>();
        int before = sorted.size();
        System.out.println("Before merging: " + before);
        IPAddress current = null;
        int counter = 0;
        sorted.removeIf(str->str.startsWith("#"));
        for (String rule : sorted) {
            if (current == null) {
                current = toCIDR(rule);
                //System.out.println("Re-assigning current base to " + current);
                continue;
            }
            if (toIP(rule).getPrefixLength() != null) {
                createdCIDR.add(rule);
                continue;
            }

            if (toCIDR(rule).equals(current)) {
                counter++;
            } else {
                counter = 0;
                current = toCIDR(rule);
                //System.out.println("Re-assigning current base to " + current);
                continue;
            }
            if(current.isIPv4()) {
                if (counter >= MERGE_TO_CIDR_AMOUNT_IPV4) {
                    createdCIDR.add(current.toString());
                }
            }else{
                if (counter >= MERGE_TO_CIDR_AMOUNT_IPV6) {
                    createdCIDR.add(current.toString());
                }
            }
        }
        createdCIDR.forEach(cidr -> {
            var base = toCIDR(cidr);
            sorted.removeIf(ip ->{
                IPAddress address = toIP(ip);
                if(address == null){
                    System.out.println("(Unresolved data) "+ip);
                    return true;
                }
                return base.contains(address);
            });
            //sorted.removeIf(ip -> base.equals(toCIDR(ip)));
        });

        var builder = new StringJoiner("\n");
        builder.add("# [START] Auto merged CIDR - IPV4: " + IPV4_PREFIX_LENGTH + ", IPV6: " + IPV6_PREFIX_LENGTH);
        createdCIDR.forEach(builder::add);
        builder.add("# [END] Auto merged CIDR - IPV4: " + IPV4_PREFIX_LENGTH + ", IPV6: " + IPV6_PREFIX_LENGTH);
        sorted.forEach(builder::add);
        String data = builder.toString();
        Files.writeString(file.toPath(),data);
        System.out.println("CIDR count: " + createdCIDR.size());
        System.out.println("After merging: " + data.lines().count());
    }

    private static IPAddress toIP(String ip) {
        return new IPAddressString(ip).getAddress();
    }

    private static IPAddress toCIDR(String cidr) {
        IPAddress ipAddress = toIP(cidr);
        if (ipAddress.getPrefixLength() != null) {
            return ipAddress;
        }
        ;
        if (ipAddress.isIPv4()) {
            ipAddress = ipAddress.setPrefixLength(IPV4_PREFIX_LENGTH);
        } else {
            ipAddress = ipAddress.setPrefixLength(IPV6_PREFIX_LENGTH);
        }
        return ipAddress.toZeroHost();
    }
}
