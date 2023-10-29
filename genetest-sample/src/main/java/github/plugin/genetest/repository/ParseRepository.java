package github.plugin.genetest.repository;

import github.plugin.genetest.App;
import github.plugin.genetest.model.ParseModel;
import jdk.nashorn.internal.runtime.arrays.ArrayIndex;
import jdk.nashorn.internal.runtime.linker.Bootstrap;

import java.text.Format;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;


public class ParseRepository {

    public ParseModel selectById(String str) {
        return null;
    }

    public ParseModel selectById(Integer str) {
        return null;
    }


    public List<ParseModel> selectById(Map<String, Map<ArrayIndex, App>> options) {
        if (options.isEmpty()) {
            return new ArrayList<>();
        } else {
            return Collections.singletonList(new ParseModel());
        }
    }

    public List<Map<ArithmeticException, Bootstrap>> selectById(List<Format> options) {
        if (options.isEmpty()) {
            return new ArrayList<>();
        } else {
            return null;
        }
    }

}
