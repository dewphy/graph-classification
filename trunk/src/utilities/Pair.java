package utilities;

public class Pair<T1, T2> {
  private final T1 t1;
  private final T2 t2;

  public Pair(T1 t1, T2 t2) {
      super();
      this.t1 = t1;
      this.t2 = t2;
  }

  public int hashCode() {
      int hashT1 = t1 != null ? t1.hashCode() : 0;
      int hashT2 = t2 != null ? t2.hashCode() : 0;

      return (hashT1 + hashT2) * hashT2 + hashT1;
  }

  @SuppressWarnings("unchecked")
	public boolean equals(Object obj) {
      if (obj instanceof Pair) {
              Pair pair = (Pair) obj;
              return ((this.t1 == pair.t1
              		|| (this.t1 != null && pair.t1 != null && this.t1.equals(pair.t1)))
              		&& (this.t2 == pair.t2
              		|| (this.t2 != null && pair.t2 != null && this.t2.equals(pair.t2))) );
      } else {
      	return false;
      }
  }
  public String toString() { 
         return "(" + t1 + ", " + t2 + ")"; 
  }

  public T1 getT1() {
      return t1;
  }
  
  public T2 getT2() {
  	return t2;
  }
}