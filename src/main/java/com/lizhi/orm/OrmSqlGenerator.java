package com.lizhi.orm;

import com.lizhi.orm.param.OrmParam;
import com.lizhi.orm.param.Param;
import com.lizhi.orm.param.QueryJoinParam;
import com.lizhi.orm.param.QueryParam;
import com.lizhi.orm.term.SortTerm;
import com.lizhi.orm.term.Term;
import org.apache.ibatis.mapping.ResultMapping;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * sql generator
 */
public class OrmSqlGenerator {

    private static SqlSessionFactory sqlSession;

    private static final OrmSqlGenerator ORM_SQL_GENERATOR = new OrmSqlGenerator();


    /**
     * resultMapId:(column:object)
     */
    protected static final Map<String, Map<String, ResultMapping>> COLUMN_RESULTMAPPING_MAPS = new HashMap<>();

    /**
     * resultMapId:(property:object)
     */
    protected static final Map<String, Map<String, ResultMapping>> PROPERTY_RESULTMAPPING_MAPS = new HashMap<>();


    protected static final Map<String, List<ResultMapping>> RESULTMAPPINGS = new HashMap<>();

    protected static final Map<String, String> SELECT_FIELD = new HashMap<>();

    protected static final Map<String, String> UPDATE_FIELD = new HashMap<>();

    protected static final Map<String, String> INSERT_FIELD = new HashMap<>();

    protected static final Map<String, String> INSERT_BODY = new HashMap<>();

    private OrmSqlGenerator() {
    }

    public static OrmSqlGenerator instance() {
        return ORM_SQL_GENERATOR;
    }

    public void processSqlSession(SqlSessionFactory sqlSession) {
        this.sqlSession = sqlSession;
        for (String resultMapId : sqlSession.getConfiguration().getResultMapNames()) {
            /**获取resultMapId的list ResultMapping*/
            List<ResultMapping> resultMapping = sqlSession.getConfiguration().getResultMap(resultMapId).getResultMappings().stream().filter(d -> {
                return StringUtils.isEmpty(d.getNestedQueryId());
            }).collect(Collectors.toList());

            RESULTMAPPINGS.put(resultMapId, resultMapping);

            ORM_SQL_GENERATOR.initResultMap(resultMapId);
        }
    }

    /**
     * 初始化语句，一些固定的值
     *
     * @param resultMapId
     */
    public void initResultMap(String resultMapId) {

        List<ResultMapping> resultMapping = RESULTMAPPINGS.get(resultMapId);

        /**初始化key（字段名-属性）*/
        COLUMN_RESULTMAPPING_MAPS.put(resultMapId, resultMapping.stream().collect(Collectors.toMap(ResultMapping::getColumn, d -> d)));

        /**初始化key（属性名-属性）*/
        PROPERTY_RESULTMAPPING_MAPS.put(resultMapId, resultMapping.stream().collect(Collectors.toMap(ResultMapping::getProperty, d -> d)));

        /**初始化select固定的字符串*/
        SELECT_FIELD.put(resultMapId, createSelectField(resultMapId, null, QueryParam.CONTAIN_NONE ,null));

        INSERT_FIELD.put(resultMapId, createInsertField(resultMapping));

        INSERT_BODY.put(resultMapId, createInsertBody(resultMapping));

        UPDATE_FIELD.put(resultMapId, createUpdateaField(resultMapping));
    }

    public String createSelectField(String resultMapId,String typeAlias) {
        if(typeAlias == null){
            return SELECT_FIELD.get(resultMapId);
        }else {
           return createSelectField(resultMapId, null, QueryParam.CONTAIN_NONE ,typeAlias);
        }
    }

    /**
     * 创建查询selelct 的字段
     * selelct  id as id....
     *
     * @param cludes
     * @param type   0:正常 1：排除 2：包含
     * @return
     */
    public String createSelectField(String resultMapId, List<String> cludes, int type ,String typeAlias) {
        if(typeAlias != null){
            typeAlias  = typeAlias+".";
        }else {
            typeAlias = "";
        }
        StringBuilder selectField = new StringBuilder();
        boolean flag = false;
        if(type == QueryParam.CONTAIN_INCLUDES){
            for(String str : cludes){
                if(flag){
                    if (!(selectField.length() == 0)) {
                        selectField.append(" , ");
                    }
                }else {
                    flag = true;
                }
                selectField.append(typeAlias+str);
            }
            return selectField.toString();
        }

        List<ResultMapping> resultMapping = RESULTMAPPINGS.get(resultMapId);
        for (ResultMapping resultMap : resultMapping) {
            boolean append = true;

            if (type == QueryParam.CONTAIN_EXCLUDES) {
                if (cludes.contains(resultMap.getColumn())) {
                    append = false;
                }
            }

            if(append){
                if (!(selectField.length() == 0)) {
                    selectField.append(" , ");
                }
//            selectField.append(resultMap.getColumn()).append(" AS ").append(resultMap.getProperty());
                selectField.append(typeAlias+resultMap.getColumn());
            }
        }
        return selectField.toString();
    }

    /**
     * 创建insert字段组成部分
     * id,labelName,parentId,articleId"
     *
     * @param resultMapping
     * @return
     */
    public String createInsertField(List<ResultMapping> resultMapping) {
        StringBuilder insertField = new StringBuilder();
        resultMapping.stream().forEach(d -> {
            if (insertField.length() != 0) {
                insertField.append(",");
            }
            insertField.append(d.getColumn());
        });
        return insertField.toString();
    }

    public String createInsertField(String resultMapId) {
        return INSERT_FIELD.get(resultMapId);
    }

    /**
     * 创建insert的body部分
     * insert into
     * "#{t_parameter.id},#{t_parameter.labelName},#{t_parameter.parentId},#{t_parameter.articleId}"
     *
     * @param resultMapping
     * @return
     */
    public String createInsertBody(List<ResultMapping> resultMapping) {
        StringBuilder insertContent = new StringBuilder();
        resultMapping.stream().forEach(d -> {
            if (insertContent.length() != 0) {
                insertContent.append(" , ");
            }
            insertContent.append("#{t_parameter." + d.getProperty() + "}");
        });
        return insertContent.toString();
    }

    public String createInsertBody(String resultMapId) {
        return INSERT_BODY.get(resultMapId);
    }

    /**
     * 创建update的fideld部分
     * update tablename set
     * id = #{t_parameter.updateObject.id}
     *
     * @param resultMapping
     * @return
     */
    public String createUpdateaField(List<ResultMapping> resultMapping) {
        /**初始化update固定的字符串*/
        final StringBuilder updateField = new StringBuilder();
        resultMapping.stream().forEach(d -> {
            if (updateField.length() != 0) {
                updateField.append(",");
            }
            updateField.append(d.getColumn()).append(" = ")
                    .append("#{t_parameter.updateObject.")
                    .append(d.getProperty())
                    .append(",jdbcType=")
                    .append(d.getJdbcType())
                    .append("}");
        });
        return updateField.toString();
    }

    public String createUpdateaField(String resultMapId, boolean isCustomEntity, Map<String, Term> terms) {
        if (isCustomEntity) {
            return UPDATE_FIELD.get(resultMapId);
        } else {
            StringBuilder updateField = new StringBuilder();
            if (terms == null || terms.isEmpty()) {
                return updateField.toString();
            }
            boolean flag = false;
            for (Map.Entry<String, Term> entry : terms.entrySet()) {
                if (flag) {
                    updateField.append(" , ");
                } else {
                    flag = true;
                }
                updateField.append(entry.getValue().getColumn())
                        .append(" = ")
                        .append("#{t_parameter.updateObject.")
                        .append(entry.getValue().getColumn())
                        .append(".value")
                        .append("}");
            }
            return updateField.toString();
        }

    }

    /**
     * @param resultMapId
     * @param param
     * @return
     */
    public String createWhereField(String resultMapId, OrmParam param) {
        final Map<String, Term> whereTerms = ((Param)param).getParams();
        final StringBuilder where = new StringBuilder();

        Map<String, ResultMapping> stringResultMappingMap = COLUMN_RESULTMAPPING_MAPS.get(resultMapId);

        /*
         * 拼接from table1 t1
         *     join table2 t2 on t1.id = t2.id1
         *     join table3 t3 on t1.id = t3.id2
         */
        if(param instanceof QueryJoinParam){
            where.append(((QueryJoinParam) param).buildJoinOnField());
        }

        /*
         *创建where部分
         * 拼接
         * where id = "1" or userName = "123"
         */
        if(!whereTerms.isEmpty()){
            where.append(" where ");
        }

        boolean isFrist = true;

        for (Map.Entry<String, Term> entry : whereTerms.entrySet()) {
            Term term = entry.getValue();
            if(isFrist){
                isFrist = false;
            }else {
                where.append(term.getType().name());
            }

            where.append(" ").append(term.getColumn())
                    .append(switchTermType(term.getTermType()));

            //分为in 和不是in 两种
            if (term.getTermType() == Term.TermType.in || term.getTermType() == Term.TermType.notin) {
                int inSize = ((Map) entry.getValue().getValue()).size();
                boolean flag = false;
                where.append(" (");
                for(int i = 0 ; i < inSize ; i ++){
                    if(flag){
                        where.append(" , ");
                    }else {
                        flag = true;
                    }
                    where.append("#{t_parameter.params.")
                            .append(entry.getKey()).append(".value.").append(i).append("}");
                }
                where.append(") ");
            } else {
                ResultMapping resultMapping = stringResultMappingMap.get(term.getColumn());
                where.append(" #{t_parameter.params.")
                        .append(entry.getKey()).append(".value");
                if(resultMapping != null){
                    where.append(",jdbcType=").append(resultMapping.getJdbcType());
                }
                where.append("} ");
            }
        }

        /*
         *拼接
         * order by
         */
        if (param instanceof QueryParam) {
            QueryParam queryParam = (QueryParam) param;

            //创建sort
            if (queryParam.getSorts() != null && !queryParam.getSorts().isEmpty()) {
                where.append("\n").append("order by ");
                boolean flag = false;
                for (Object sortTerm : queryParam.getSorts()) {
                    if (flag) {
                        where.append(" , ");
                    } else {
                        flag = true;
                    }
                    where.append(((SortTerm) sortTerm).getColumn()).append(" ")
                            .append(((SortTerm) sortTerm).getValue());
                }
            }

            /*
             *拼接
             *group by
             */
            if (queryParam.getGroups() != null && !queryParam.getGroups().isEmpty()) {
                where.append("\n").append("group by ");
                boolean flag = false;
                for (Object group : queryParam.getGroups()) {
                    if (flag) {
                        where.append(" , ");
                    } else {
                        flag = true;
                    }
                    where.append(group);
                }
            }


            /*
             *拼接
             *limit
             */
            if (queryParam.getPageNumber() != 0 || queryParam.getPageSize() != 0) {
                where.append("\n").append("limit ")
                        .append(queryParam.getPageNumber())
                        .append(",")
                        .append(queryParam.getPageNumber() + queryParam.getPageSize());
            }
        }


        return where.toString();
    }

    public final static String switchTermType(Term.TermType termType) {
        switch (termType) {
            case eq:
                return " = ";
            case neq:
                return " <> ";
            case lt:
                return " < ";
            case lte:
                return " <= ";
            case gt:
                return " > ";
            case gte:
                return " >= ";
            case isnull:
                return " is null ";
            case isvoid:
                return " = \"\" ";
            case notnull:
                return " is not null ";
            case notvoid:
                return " <> \"\" ";
            case in:
                return " in ";
            case notin:
                return " not in ";
            case like:
                return " like ";
            case notlike:
                return "not like ";
            default:
                return " = ";
        }
    }

}
