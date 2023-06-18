package pt.up.fe.comp2023.ollir;

public class ExprToOllir {
    public String value;
    public String prefix;

    public ExprToOllir(String prefix, String value) {
        this.value = value;
        this.prefix = prefix;
    }

    public ExprToOllir(){
        this.value = "";
        this.prefix = "";
    }
}
