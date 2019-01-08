package com.lizhi.controller;

import com.lizhi.bean.*;
import com.lizhi.orm.param.Param;
import com.lizhi.orm.param.QueryParam;
import com.lizhi.orm.term.SortTerm;
import com.lizhi.orm.term.Term;
import com.lizhi.service.CustomService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.lizhi.bean.ResponseMessage.ok;


public interface CustomController <M extends CustomEntity,PK > {

    CustomService<M,PK> getService();

    @ResponseStatus(HttpStatus.CREATED)
    default ResponseMessage add(@RequestBody M data) {
        return ok(getService().insert(data));
    }

    @DeleteMapping(path = "/{id:.+}")
    default ResponseMessage deleteByPrimaryKey(@PathVariable PK id) {
        return ok(getService().deleteByPK(id));
    }

    @GetMapping
    default ResponseMessage<PagerResult<M>> list(QueryParam param) {
        return ok(getService().selectPager(param));
    }

    @GetMapping("/count")
    default ResponseMessage<Integer> count(QueryParam param) {
        return ok(Integer.valueOf(getService().count(param)));
    }

    @GetMapping(path = "/{id}")
    default ResponseMessage<M> getByPrimaryKey(@PathVariable PK id) {
        return ok(assertNotNull(getService().selectByPK(id)));
    }

    default Map<String,Object> getParams(Param curdParam) {
        Map params = curdParam.getParams();
        if(params == null){
            return null;
        }
        return  (Map<String,Object>) params.values().stream().collect(Collectors.toMap(Term::getColumn, Term::getValue));
    }

    default LinkedHashMap<String,Object> getSorts(QueryParam curdParam) {
        List<SortTerm> sorts = curdParam.getSorts();
        if(sorts == null){
            return null;
        }
        return  (LinkedHashMap<String,Object>) sorts.stream().collect(Collectors.toMap(SortTerm::getColumn, SortTerm::getValue));
    }

    default List<String> getGroups(QueryParam curdParam) {
        return curdParam.getGroups();
    }


//    @GetMapping(path = "/ids")
//    default ResponseMessage<List<M>> getByPrimaryKey(@RequestParam List<PK> ids) {
//        return ok(assertNotNull(getService().selectByPK(ids)));
//    }

    static <T> T assertNotNull(T obj) {
        if (null == obj) {
            throw new RuntimeException("{data_not_exist}");
        }
        return obj;
    }

//    @PutMapping(path = "/{id}")
//    default ResponseMessage<Integer> updateByPrimaryKey(@PathVariable PK id, @RequestBody M data) {
//        E entity = getService().createEntity();
//        return ResponseMessage.ok(getService().updateByPK(id, modelToEntity(data, entity)));
//    }
//
//    @PatchMapping
//    default ResponseMessage<PK> saveOrUpdate(@RequestBody M data) {
//        E entity = getService().createEntity();
//        return ResponseMessage.ok(getService().saveOrUpdate(modelToEntity(data, entity)));
//    }

}
