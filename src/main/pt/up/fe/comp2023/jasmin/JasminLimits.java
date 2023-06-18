package pt.up.fe.comp2023.jasmin;

import org.specs.comp.ollir.Descriptor;
import org.specs.comp.ollir.Method;

import java.util.Set;
import java.util.TreeSet;

import static pt.up.fe.comp2023.jasmin.JasminBuilder.currentTotal;
import static pt.up.fe.comp2023.jasmin.JasminBuilder.stackMaxSize;


public class JasminLimits {
    static Set<Integer> auxCount = new TreeSet<>();

    static public void updateStackLimit(int counter) {
        currentTotal += counter;
        stackMaxSize = Math.max(currentTotal, stackMaxSize);
    }

    static public int updateLocalLimit(Method method) {
        auxCount.clear();
        auxCount.add(0);
        for (Descriptor descriptor : method.getVarTable().values()) {
            auxCount.add(descriptor.getVirtualReg());
        }
        return auxCount.size();
    }

    static public int getStackMaxSixe() {
        return stackMaxSize;
    }
}
