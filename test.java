import com.helger.db.jdbc.executor.DBResultRow;
public class test {
  public static void main(String[] args) {
    for(java.lang.reflect.Method m : DBResultRow.class.getMethods()) {
      System.out.println(m.getName());
    }
  }
}
