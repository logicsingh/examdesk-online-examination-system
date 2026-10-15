package edu.bsc.exam;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
public final class Scoring {
 private Scoring() {}
 public record Key(String correctOption, BigDecimal marks) {}
 public static BigDecimal score(Map<Long,Key> key, Map<Long,String> submitted) {
  BigDecimal total=BigDecimal.ZERO;
  for (var entry:key.entrySet()) {
   String selected=submitted.get(entry.getKey());
   if(selected!=null && selected.equalsIgnoreCase(entry.getValue().correctOption())) total=total.add(entry.getValue().marks());
  }
  return total.setScale(2,RoundingMode.HALF_UP);
 }
}
