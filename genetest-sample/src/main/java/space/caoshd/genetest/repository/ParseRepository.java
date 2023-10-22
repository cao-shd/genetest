package space.caoshd.genetest.repository;

import space.caoshd.genetest.model.ParseModel;

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

    public List<ParseModel> selectById(Map<String, Object> options) {
        if (options.isEmpty()) {
            return new ArrayList<>();
        } else {
            return Collections.singletonList(new ParseModel());
        }
    }
}
