package halo.query;

import halo.query.dal.*;
import halo.query.mapping.EntityTableInfo;
import halo.query.mapping.EntityTableInfoFactory;
import halo.query.mapping.SQLMapper;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.jdbc.core.RowMapper;

import java.lang.reflect.Field;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unchecked")
public class Query {

    private static Query instance;

    protected JdbcSupport jdbcSupport;

    public Query() {
        instance = this;
    }

    public static Query getInstance() {
        if (instance == null) {
            throw new RuntimeException("must create " + Query.class.getName());
        }
        return instance;
    }

    /**
     * select count(*) 查询。查询中的表别名必须与表名相同
     *
     * @param clazzes   查询对象类型数组
     * @param afterFrom from table 之后的sql,例如select * from table where uid=?
     *                  order name desc, afterFrom为where uid=? order name desc
     * @param values    参数化查询值
     * @return sql统计数字
     */
    public int count(Class<?>[] clazzes, String afterFrom, Object[] values) {
        return jdbcSupport.num(SqlBuilder.buildCountSQL(clazzes, afterFrom), values).intValue();
    }

    /**
     * @param clazzes   查询对象类型数组
     * @param afterFrom from table 之后的sql,例如select * from table where uid=?
     *                  order name desc, afterFrom为where uid=? order name desc
     * @param values    参数化查询值集合
     * @return 查询数量
     */
    public int count(Class<?>[] clazzes, String afterFrom, List<?> values) {
        return this.count(clazzes, afterFrom, buildArgs(values));
    }

    /**
     * select count(*) 查询
     *
     * @param clazz     查询对象类型
     * @param afterFrom from table 之后的sql,例如select * from table where uid=?
     *                  order name desc, afterFrom为where uid=? order name desc
     * @param values    参数化查询值
     * @param <T>       对象泛型
     * @return 查询数量
     */
    public <T> int count(Class<T> clazz, String afterFrom, Object[] values) {
        return jdbcSupport.num(SqlBuilder.buildCountSQL(clazz, afterFrom), values).intValue();
    }

    /**
     * select count(*) 查询
     *
     * @param clazz     查询对象类型
     * @param afterFrom from table 之后的sql,例如select * from table where uid=?
     *                  order name desc, afterFrom为where uid=? order name desc
     * @param values    参数化查询值集合
     * @param <T>       对象泛型
     * @return 查询数量
     */
    public <T> int count2(Class<T> clazz, String afterFrom, List<?> values) {
        return this.count(clazz, afterFrom, buildArgs(values));
    }

    /**
     * 对sql中有 in (?,?)的count封装，目前只支持 单个in
     *
     * @param clazz     操作的类
     * @param afterFrom from之后的sql，例如 where col=?,但是不包括 inColumn
     * @param inColumn  进行in sql操作的列
     * @param values    ?替换符对应的参数，不包括inColumn的参数
     * @param inValues  inColumn对应的参数
     * @param <T>       集合中对象泛型
     * @return 查询数量
     */
    public <T> int countInValues(Class<T> clazz, String afterFrom, String inColumn, Object[] values, Object[] inValues) {
        if (inValues == null || inValues.length == 0) {
            DALStatus.processDALConClose();
            return 0;
        }
        List<Object> paramlist = new ArrayList<Object>();
        if (values != null) {
            for (int i = 0; i < values.length; i++) {
                paramlist.add(values[i]);
            }
        }
        for (int i = 0; i < inValues.length; i++) {
            paramlist.add(inValues[i]);
        }

        String _where;
        if (afterFrom == null) {
            _where = "where ";
        } else {
            _where = afterFrom + " and ";
        }
        return count(clazz, _where + createInSql(inColumn, inValues.length), buildArgs(paramlist));
    }

    /**
     * 对sql中有 in (?,?)的count封装，目前只支持 单个in
     *
     * @param clazz     操作的类
     * @param afterFrom from之后的sql，例如 where col=?,但是不包括 inColumn
     * @param inColumn  进行in sql操作的列
     * @param values    ?替换符对应的参数，不包括inColumn的参数
     * @param inValues  inColumn对应的参数
     * @param <T>       集合中对象泛型
     * @return 查询数量
     */
    public <T> int countInValues2(Class<T> clazz, String afterFrom, String inColumn, List<?> values, List<?> inValues) {
        return countInValues(clazz, afterFrom, inColumn, buildArgs(values), buildArgs(inValues));
    }

    /**
     * sql select
     *
     * @param clazz     查询结果类型
     * @param afterFrom from之后的sql，例如 where col=? order by uid desc,
     * @param values    参数化查询值
     * @param rowMapper spring {@link RowMapper} 对象
     * @param <T>       泛型
     * @return 查询结果 T 类型的集合
     */
    public <T> List<T> list(Class<T> clazz, String afterFrom, Object[] values, RowMapper<T> rowMapper) {
        return jdbcSupport.list(SqlBuilder.buildListSQL(clazz, afterFrom), values, rowMapper);
    }

    /**
     * sql select
     *
     * @param clazz     查询结果类型
     * @param afterFrom from之后的sql，例如 where col=? order by uid desc,
     * @param values    参数化查询值集合
     * @param rowMapper spring {@link RowMapper} 对象
     * @param <T>       泛型
     * @return 查询结果 T 类型的集合
     */
    public <T> List<T> list2(Class<T> clazz, String afterFrom,
                             List<?> values, RowMapper<T> rowMapper) {
        return this.list(clazz, afterFrom, buildArgs(values), rowMapper);
    }

    /**
     * sql select
     *
     * @param clazz     查询结果类型
     * @param afterFrom from之后的sql，例如 where col=? order by uid desc,
     * @param values    参数化查询值
     * @param <T>       泛型
     * @return 查询结果 T 类型的集合
     */
    public <T> List<T> list(Class<T> clazz, String afterFrom, Object[] values) {
        return this.list(clazz, afterFrom, values, getRowMapper(clazz));
    }

    /**
     * sql select
     *
     * @param clazz     查询结果类型
     * @param afterFrom from之后的sql，例如 where col=? order by uid desc,
     * @param values    参数化查询值集合
     * @param <T>       泛型
     * @return 查询结果 T 类型的集合
     */
    public <T> List<T> list2(Class<T> clazz, String afterFrom, List<?> values) {
        return this.list(clazz, afterFrom, buildArgs(values));
    }

    /**
     * 使用 column in (?,?)的方式来获得集合数据
     *
     * @param clazz     操作的类
     * @param afterFrom from之后的sql，例如 where col=?,但是不包括 inColumn
     * @param inColumn  进行in sql操作的列
     * @param values    ?替换符对应的参数，不包括inColumn的参数
     * @param inValues  inColumn对应的参数
     * @param <T>       集合中对象泛型
     * @return 查询结果 T 类型的集合
     */
    public <T> List<T> listInValues(Class<T> clazz, String afterFrom,
                                    String inColumn, Object[] values, Object[] inValues) {
        return this.listInValues(clazz, afterFrom, inColumn, null, values, inValues);
    }

    public <T> List<T> listInValues2(Class<T> clazz, String afterFrom, String inColumn, String afterWhere, List<?> values, List<?> inValues) {
        return this.listInValues(clazz, afterFrom, inColumn, afterWhere, buildArgs(values), buildArgs(inValues));
    }

    /**
     * 使用 column in (?,?)的方式来获得集合数据
     *
     * @param clazz      操作的类
     * @param afterFrom  from之后的sql，例如 where col=?,但是不包括 inColumn
     * @param inColumn   进行in sql操作的列
     * @param afterWhere 条件语句之后的例如 order by, group by 等语句
     * @param values     ?替换符对应的参数，不包括inColumn的参数
     * @param inValues   inColumn对应的参数
     * @param <T>        集合中对象泛型
     * @return 查询结果 T 类型的集合
     */
    public <T> List<T> listInValues(Class<T> clazz, String afterFrom, String inColumn, String afterWhere, Object[] values, Object[] inValues) {
        if (inValues == null || inValues.length == 0) {
            DALStatus.processDALConClose();
            return new ArrayList<T>(0);
        }
        List<Object> paramlist = new ArrayList<Object>();
        if (values != null) {
            for (int i = 0; i < values.length; i++) {
                paramlist.add(values[i]);
            }
        }
        for (int i = 0; i < inValues.length; i++) {
            paramlist.add(inValues[i]);
        }
        StringBuilder sb = new StringBuilder();
        if (afterFrom == null) {
            sb.append("where ");
        } else {
            sb.append(afterFrom).append(" and ");
        }
        sb.append(createInSql(inColumn, inValues.length));
        if (afterWhere != null) {
            sb.append(' ').append(afterWhere);
        }
        return list(clazz, sb.toString(), buildArgs(paramlist));
    }

    /**
     * 使用 column in (?,?)的方式来获得集合数据
     *
     * @param clazz     操作的类
     * @param afterFrom from之后的sql，例如 where col=?,但是不包括 inColumn
     * @param inColumn  进行in sql操作的列
     * @param values    ?替换符对应的参数，不包括inColumn的参数
     * @param inValues  inColumn对应的参数
     * @param <T>       集合中对象泛型
     * @return 查询结果 T 类型的集合
     */
    public <T> List<T> listInValues2(Class<T> clazz, String afterFrom,
                                     String inColumn, List<?> values, List<?> inValues) {
        return listInValues(clazz, afterFrom, inColumn, buildArgs(values), buildArgs(inValues));
    }

    /**
     * @param clazz     操作的类
     * @param afterFrom from之后的sql，例如 where col=?,但是不包括 inColumn
     * @param inColumn  进行in sql操作的列
     * @param values    ?替换符对应的参数，不包括inColumn的参数
     * @param inValues  inColumn对应的参数
     * @param <E>       map中key的类型
     * @param <T>       集合中对象泛型
     * @return map对象
     */
    public <E, T> Map<E, T> map(Class<T> clazz, String afterFrom, String inColumn, Object[] values, Object[]
            inValues) {
        Map<E, T> map = new HashMap<E, T>(0);
        if (inValues == null || inValues.length == 0) {
            DALStatus.processDALConClose();
            return map;
        }
        List<T> list = listInValues(clazz, afterFrom, inColumn, values, inValues);
        EntityTableInfo<T> entityTableInfo = getEntityTableInfo(clazz);
        Field field = entityTableInfo.getField(inColumn);
        for (T t : list) {
            map.put((E) entityTableInfo.getFieldValue(t, field), t);
        }
        return map;
    }

    /**
     * @param clazz     操作的类
     * @param afterFrom from之后的sql，例如 where col=?,但是不包括 inColumn
     * @param inColumn  进行in sql操作的列
     * @param values    ?替换符对应的参数，不包括inColumn的参数
     * @param inValues  incolumn对应的参数
     * @param <E>       map中key的类型
     * @param <T>       集合中对象泛型
     * @return map对象
     */
    public <E, T> Map<E, T> map2(Class<T> clazz, String afterFrom,
                                 String inColumn, List<?> values, List<?> inValues) {
        return map(clazz, afterFrom, inColumn, buildArgs(values), buildArgs(inValues));
    }

    public static String createInSql(String column, int argCount) {
        return SqlBuilder.createInSql(column, argCount);
    }

    /**
     * delete sql.根据条件删除.例如: delete table where field0=? and ....
     *
     * @param clazz     要删除的对象类型
     * @param afterFrom delete table 之后的语句,例如:delete table where field0=?,afterFrom为where field0=?
     * @param values    参数化查询值
     * @param <T>       对象泛型
     * @return 删除的记录数
     */
    public <T> int delete(Class<T> clazz, String afterFrom, Object[] values) {
        return this.jdbcSupport.update(SqlBuilder.buildDeleteSQL(clazz, afterFrom), values);
    }

    /**
     * 批量删除
     *
     * @param clazz      要删除的对象
     * @param afterFrom  delete table 之后的语句,例如:delete table where field0=?,afterFrom为where field0=?
     * @param valuesList 批量操作的参数集合
     * @param <T>        类泛型
     * @return delete result
     */
    public <T> int[] batchDelete(Class<T> clazz, String afterFrom, List<Object[]> valuesList) {
        return this.jdbcSupport.batchUpdate(SqlBuilder.buildDeleteSQL(clazz, afterFrom), valuesList);
    }

    /**
     * 删除
     *
     * @param clazz     要删除的对象
     * @param afterFrom delete table 之后的语句,例如:delete table where field0=?,afterFrom为where field0=?
     * @param values    参数
     * @param <T>       类泛型
     * @return delete result
     */
    public <T> int delete2(Class<T> clazz, String afterFrom, List<?> values) {
        return this.delete(clazz, afterFrom, buildArgs(values));
    }

    /**
     * delete sql,返回删除的记录数量
     *
     * @param t   要删除的对象，必须有id
     * @param <T> 对象泛型
     * @return 删除的记录数
     */
    public <T> int delete(T t) {
        SQLMapper<T> mapper = getSqlMapper(t.getClass());
        return this.deleteById(t.getClass(), mapper.getIdParams(t));
    }

    /**
     * delete sql,根据id删除。返回删除的记录数量
     *
     * @param clazz    要删除的对象的类型
     * @param idValues 主键id值
     * @param <T>      对象泛型
     * @return sql操作失败的异常
     */
    public <T> int deleteById(Class<T> clazz, Object[] idValues) {
        return this.jdbcSupport.update(SqlBuilder.buildDeleteSQL(clazz), idValues);
    }


    /**
     * 批量insert,如果表存在联合主键，并且其中的一个主键是自增长，那么需要把自增长的字段标识为@Id(0)
     *
     * @param list 批量创建的对象
     * @param <T>  对象类型
     * @return 返回自增id，如果id不是自增，就返回值为0的集合
     */
    public <T> List<T> batchInsert(final List<T> list) {
        if (list == null || list.isEmpty()) {
            DALStatus.processDALConClose();
            throw new RuntimeException("batchInsert list must be not empty");
        }
        EntityTableInfo<T> info = getEntityTableInfo(list.get(0).getClass());
        String sql = SqlBuilder.buildInsertSQL(list.get(0).getClass(), true);
        List<Object[]> valuesList = new ArrayList<Object[]>();
        try {
            for (T t : list) {
                Object[] params = new Object[info.getTableFields().size()];
                int i = 0;
                for (Field field : info.getTableFields()) {
                    params[i++] = field.get(t);
                }
                valuesList.add(params);
            }
        } catch (IllegalAccessException e) {
            DALStatus.processDALConClose();
            throw new RuntimeException(e);
        }
        List<Number> ids = this.jdbcSupport.batchInsert(sql, valuesList, true);
        if (info.getIdFields().isEmpty()) {
            return list;
        }
        if (ids.size() != list.size()) {
            return list;
        }
        Field idField = info.getIdFields().get(0);
        try {
            for (int i = 0; i < list.size(); i++) {
                T t = list.get(i);
                if (this.isNumberIdType(idField)) {
                    Object idValue = idField.get(t);
                    if (idValue == null) {
                        this.setIdValue(t, idField, ids.get(i));
                    } else {
                        if (((Number) idValue).longValue() <= 0 && ids.get(i).longValue() > 0) {
                            this.setIdValue(t, idField, ids.get(i));
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return list;
    }

    /**
     * insert sql
     *
     * @param t   insert的对象
     * @param <T> 对象泛型
     */
    public <T> void insert(T t) {
        SQLMapper<T> mapper = getSqlMapper(t.getClass());
        this.jdbcSupport.insert(SqlBuilder.buildInsertSQL(t.getClass(), true), mapper.getParamsForInsert(t, true), false);
    }

    /**
     * replace into sql,此操作，如果是更新数据，将不会返回自增id
     *
     * @param t   数据对象
     * @param <T> 泛型
     * @return 当进行replace操作时，返回0
     */
    public <T> Number replace(T t) {
        return this.insertForNumber(t, InsertFlag.REPLACE_INTO);
    }

    /**
     * insert ignore into sql
     *
     * @param t   数据对象
     * @param <T> 泛型
     * @return 当插入不成功时，返回0
     */
    public <T> Number insertIgnore(T t) {
        return this.insertForNumber(t, InsertFlag.INSERT_IGNORE_INTO);
    }

    /**
     * @param t          数据对象
     * @param insertFlag 操作标识
     * @param <T>        泛型
     * @return 返回自增id，如果没有自增id，返回0
     */
    public <T> Number insertForNumber(T t, InsertFlag insertFlag) {
        EntityTableInfo<T> info = getEntityTableInfo(t.getClass());
        SQLMapper<T> mapper = getSqlMapper(t.getClass());
        if (info.getIdFields().size() > 1) {
            this.jdbcSupport.insert(SqlBuilder.buildInsertSQL(t.getClass(), true, insertFlag), mapper.getParamsForInsert(t, true), false);
            return 0;
        }
        if (info.getIdFields().isEmpty()) {
            this.jdbcSupport.insert(SqlBuilder.buildInsertSQL(t.getClass(), true, insertFlag), mapper.getParamsForInsert(t, true), false);
            return 0;
        }
        Field idField = info.getIdFields().get(0);
        Object idValue;
        try {
            idValue = idField.get(t);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        // id 为数字时，只支持 int long
        if (this.isNumberIdType(idField)) {
            if (idValue == null) {
                idValue = 0;
            }
            Number num = (Number) idValue;
            // id = 0,需要获得自增id
            if (num.longValue() <= 0) {
                // 为自增id方式
                Number n = (Number) (this.jdbcSupport.insert(SqlBuilder.buildInsertSQL(t.getClass(), false, insertFlag), mapper.getParamsForInsert(t, false), true));
                if (n != null && n.intValue() > 0) {
                    this.setIdValue(t, idField, n);
                }
                return n;
            }
            // id>0,不需要赋值，返回0
            this.jdbcSupport.insert(SqlBuilder.buildInsertSQL(t.getClass(), true, insertFlag), mapper.getParamsForInsert(t, true), false);
            return 0;
        }
        // 非数字id时,不需要赋值
        this.jdbcSupport.insert(SqlBuilder.buildInsertSQL(t.getClass(), true, insertFlag), mapper.getParamsForInsert(t, true), false);
        return 0;
    }

    /**
     * insert sql,返回自增数字id，联合主键的表，返回0. 如果表没有主键，直接insert,返回0
     *
     * @param t   insert的对象
     * @param <T> 对象泛型
     * @return insert之后的自增数字
     */
    public <T> Number insertForNumber(T t) {
        return this.insertForNumber(t, InsertFlag.INSERT_INTO);
    }


    private boolean isNumberIdType(Field field) {
        Class<?> cls = field.getType();
        return cls.equals(int.class) || cls.equals(Integer.class) || cls.equals(long.class) || cls.equals(Long.class) || cls.equals(BigInteger.class);
    }

    private <T> void setIdValue(T t, Field idField, Number n) {
        try {
            if (idField.getType().equals(Integer.class) || idField.getType().equals(int.class)) {
                idField.set(t, n.intValue());
            } else if (idField.getType().equals(Long.class) || idField.getType().equals(long.class)) {
                idField.set(t, n.longValue());
            } else if (idField.getType().equals(Double.class) || idField.getType().equals(double.class)) {
                idField.set(t, n.doubleValue());
            } else if (idField.getType().equals(Float.class) || idField.getType().equals(float.class)) {
                idField.set(t, n.floatValue());
            } else if (idField.getType().equals(Short.class) || idField.getType().equals(short.class)) {
                idField.set(t, n.shortValue());
            } else if (idField.getType().equals(BigInteger.class)) {
                //对于 BigInteger 支持的不好，不建议使用
                idField.set(t, BigInteger.valueOf(n.longValue()));
            } else {
                //其他数组类型不考虑
                throw new IllegalArgumentException("unsupported idField type:" + idField.getType().getName());
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * mysql的分页查询。查询中的表别名必须与表名相同
     *
     * @param clazzes   查询对象类型数组
     * @param afterFrom from table 之后的sql,例如select * from table where uid=?
     *                  order name desc, afterFrom为where uid=? order name
     * @param begin     开始位置
     * @param size      查询数量
     * @param values    参数化查询值
     * @param rowMapper spring RowMapper
     * @param <T>       对象泛型
     * @return 查询结果 T 类型的集合
     */
    public <T> List<T> mysqlList(Class<?>[] clazzes, String afterFrom, int begin, int size, Object[] values, RowMapper<T> rowMapper) {
        return jdbcSupport.list(SqlBuilder.buildMysqlListSQL(clazzes, afterFrom, begin, size), values, rowMapper);
    }

    /**
     * mysql的分页查询。查询中的表别名必须与表名相同
     *
     * @param clazzes   查询对象类型数组
     * @param afterFrom from table 之后的sql,例如select * from table where uid=?
     *                  order name desc, afterFrom为where uid=? order name
     * @param begin     开始位置
     * @param size      查询数量
     * @param values    参数化查询值集合
     * @param rowMapper spring RowMapper
     * @param <T>       对象泛型
     * @return 查询结果 T 类型的集合
     */
    public <T> List<T> mysqlList2(Class<?>[] clazzes, String afterFrom,
                                  int begin, int size, List<?> values, RowMapper<T> rowMapper) {
        return this.mysqlList(clazzes, afterFrom, begin, size, buildArgs(values), rowMapper);
    }

    /**
     * mysql的分页查询。
     *
     * @param clazz     查询对象类型
     * @param afterFrom from table 之后的sql,例如select * from table where uid=?
     *                  order name desc, afterFrom为where uid=? order name
     * @param begin     开始位置
     * @param size      查询数量
     * @param values    参数化查询值
     * @param <T>       对象泛型
     * @return 查询结果 T 类型的集合
     */
    public <T> List<T> mysqlList(Class<T> clazz, String afterFrom, int begin, int size, Object[] values) {
        return this.mysqlList(clazz, afterFrom, begin, size, values, getRowMapper(clazz));
    }

    /**
     * mysql的分页查询。
     *
     * @param clazz     查询对象类型
     * @param afterFrom from table 之后的sql,例如select * from table where uid=?
     *                  order name desc, afterFrom为where uid=? order name
     * @param begin     开始位置
     * @param size      查询数量集合
     * @param values    参数化查询值
     * @param <T>       对象泛型
     * @return 查询结果 T 类型的集合
     */
    public <T> List<T> mysqlList2(Class<T> clazz, String afterFrom,
                                  int begin, int size, List<?> values) {
        return this.mysqlList(clazz, afterFrom, begin, size, buildArgs(values));
    }

    /**
     * mysql的分页查询。
     *
     * @param clazz     查询对象类型
     * @param afterFrom from table 之后的sql,例如select * from table where uid=?
     *                  order name desc, afterFrom为where uid=? order name
     * @param begin     开始位置
     * @param size      查询数量
     * @param values    参数化查询值
     * @param rowMapper spring RowMapper
     * @param <T>       对象泛型
     * @return 查询结果 T 类型的集合
     */
    public <T> List<T> mysqlList(Class<T> clazz, String afterFrom, int begin, int size, Object[] values, RowMapper<T> rowMapper) {
        return jdbcSupport.list(SqlBuilder.buildMysqlListSQL(clazz, afterFrom, begin, size), values, rowMapper);
    }

    /**
     * mysql的分页查询。
     *
     * @param clazz     查询对象类型
     * @param afterFrom from table 之后的sql,例如select * from table where uid=?
     *                  order name desc, afterFrom为where uid=? order name
     * @param begin     开始位置
     * @param size      查询数量
     * @param values    参数化查询值集合
     * @param rowMapper spring RowMapper
     * @param <T>       对象泛型
     * @return 查询结果 T 类型的集合
     */
    public <T> List<T> mysqlList2(Class<T> clazz, String afterFrom,
                                  int begin, int size, List<?> values, RowMapper<T> rowMapper) {
        return this.mysqlList(clazz, afterFrom, begin, size, buildArgs(values), rowMapper);
    }

    /**
     * select sql 返回对象
     *
     * @param clazz     查询对象类型
     * @param afterFrom from table 之后的sql,例如select * from table where uid=?
     *                  order name desc, afterFrom为where uid=? order name desc
     * @param values    参数化查询值
     * @param <T>       对象泛型
     * @return 查询 T 类型对象，null表示没有搜索结果
     */
    public <T> T obj(Class<T> clazz, String afterFrom, Object[] values) {
        return this.obj(clazz, afterFrom, values, getRowMapper(clazz));
    }

    /**
     * select sql 返回对象
     *
     * @param clazz     查询对象类型
     * @param afterFrom from table 之后的sql,例如select * from table where uid=?
     *                  order name desc, afterFrom为where uid=? order name desc
     * @param values    参数化查询值集合
     * @param <T>       对象泛型
     * @return 查询 T 类型对象，null表示没有搜索结果
     */
    public <T> T obj2(Class<T> clazz, String afterFrom, List<?> values) {
        return this.obj(clazz, afterFrom, buildArgs(values));
    }

    /**
     * select sql 返回对象
     *
     * @param clazz     查询对象类型
     * @param afterFrom from table 之后的sql,例如select * from table where uid=?
     *                  order name desc, afterFrom为where uid=? order name desc
     * @param values    参数化查询值
     * @param rowMapper spring RowMapper
     * @param <T>       对象泛型
     * @return 查询 T 类型对象，null表示没有搜索结果
     */
    public <T> T obj(Class<T> clazz, String afterFrom, Object[] values, RowMapper<T> rowMapper) {
        List<T> list = jdbcSupport.list(SqlBuilder.buildObjSQL(clazz, afterFrom), values, rowMapper);
        if (list.isEmpty()) {
            return null;
        }
        if (list.size() == 1) {
            return list.get(0);
        }
        throw new IncorrectResultSizeDataAccessException(1, list.size());
    }

    /**
     * select sql 返回对象
     *
     * @param clazz     查询对象类型
     * @param afterFrom from table 之后的sql,例如select * from table where uid=?
     *                  order name desc, afterFrom为where uid=? order name desc
     * @param values    参数化查询值集合
     * @param rowMapper spring RowMapper
     * @param <T>       对象泛型
     * @return 查询 T 类型对象，null表示没有搜索结果
     */
    public <T> T obj2(Class<T> clazz, String afterFrom, List<?> values,
                      RowMapper<T> rowMapper) {
        return this.obj(clazz, afterFrom, buildArgs(values), rowMapper);
    }

    /**
     * select sql 根据id查询，返回对象
     *
     * @param clazz   查询对象类型
     * @param idValue id参数
     * @param <T>     对象泛型
     * @return 查询 T 类型对象，null表示没有搜索结果
     */
    public <T> T objById(Class<T> clazz, Object idValue) {
        return this.objByIds(clazz, new Object[]{idValue});
    }

    /**
     * select sql 根据id查询,并且实现sql select ... for update 功能
     *
     * @param clazz     查询对象类型
     * @param idValue   id参数
     * @param forUpdate 是否锁数据
     * @param <T>       对象泛型
     * @return 查询 T 类型对象，null表示没有搜索结果
     */
    public <T> T objById(Class<T> clazz, Object idValue, boolean forUpdate) {
        return this.objByIds(clazz, new Object[]{idValue}, forUpdate, getRowMapper(clazz));
    }

    /**
     * select 根据id查询对象，并使用for update 锁定该行数据
     *
     * @param clazz   查询对象类型
     * @param idValue id参数
     * @param <T>     对象泛型
     * @return 查询 T 类型对象，null表示没有搜索结果
     */
    public <T> T objByIdForUpdate(Class<T> clazz, Object idValue) {
        return this.objByIds(clazz, new Object[]{idValue}, true, getRowMapper(clazz));
    }

    /**
     * select sql 根据id查询，返回对象
     *
     * @param clazz    查询对象类型
     * @param idValues id参数
     * @param <T>      对象泛型
     * @return 查询 T 类型对象，null表示没有搜索结果
     */
    public <T> T objByIds(Class<T> clazz, Object[] idValues) {
        return this.objByIds(clazz, idValues, false, getRowMapper(clazz));
    }

    /**
     * select 根据id查询对象,并使用for update 锁定该行数据
     *
     * @param clazz    查询对象类型
     * @param idValues id参数
     * @param <T>      对象泛型
     * @return 查询 T 类型对象，null表示没有搜索结果
     */
    public <T> T objByIdsForUpdate(Class<T> clazz, Object[] idValues) {
        return this.objByIds(clazz, idValues, true, getRowMapper(clazz));
    }

    /**
     * select 根据id查询对象,并使用for update 锁定该行数据
     *
     * @param clazz     查询对象类型
     * @param idValues  id参数
     * @param forUpdate 是否锁对象
     * @param <T>       对象泛型
     * @return 查询 T 类型对象，null表示没有搜索结果
     */
    public <T> T objByIds(Class<T> clazz, Object[] idValues, boolean forUpdate) {
        return this.objByIds(clazz, idValues, forUpdate, getRowMapper(clazz));
    }

    /**
     * select sql 根据id查询，返回对象
     *
     * @param clazz     查询对象类型
     * @param idValues  id参数
     * @param forUpdate 是否使用sql for update进行锁数据
     * @param rowMapper spring RowMapper
     * @param <T>       对象泛型
     * @return 查询 T 类型对象，null表示没有搜索结果
     */
    public <T> T objByIds(Class<T> clazz, Object[] idValues, boolean forUpdate, RowMapper<T> rowMapper) {
        return this.obj(clazz, SqlBuilder.buildObjByIdsSQLSeg(clazz, idValues, forUpdate), idValues, rowMapper);
    }

    /**
     * 批量更新
     *
     * @param clazz        更新的类型
     * @param updateSqlSeg sql片段,为update table 之后的sql。例如：set field0=?,field1=? where field3=?
     * @param valuesList   批量操作的参数集合
     * @param <T>          类泛型
     * @return update result
     */
    public <T> int[] batchUpdate(Class<T> clazz, String updateSqlSeg, List<Object[]> valuesList) {
        return this.jdbcSupport.batchUpdate(SqlBuilder.buildUpdateSQL(clazz, updateSqlSeg), valuesList);
    }

    /**
     * update sql，返回更新的记录数量。只更新选中的字段 例如: update table set field0=?,field1=?
     * where field3=?
     *
     * @param clazz        需要更新的类
     * @param updateSqlSeg sql片段,为update table 之后的sql。例如：set field0=?,field1=? where field3=?
     * @param values       参数化查询值
     * @param <T>          对象泛型
     * @return 更新数量
     */
    public <T> int update(Class<T> clazz, String updateSqlSeg, Object[] values) {
        return this.jdbcSupport.update(SqlBuilder.buildUpdateSQL(clazz, updateSqlSeg), values);
    }

    /**
     * update sql，返回更新的记录数量。只更新选中的字段 例如: update table set field0=?,field1=?
     *
     * @param clazz        需要更新的类
     * @param updateSqlSeg sql片段,为update table 之后的sql。例如：set field0=?,field1=? where field3=?
     * @param values       参数化查询值
     * @param <T>          类泛型
     * @return 更新数量
     */
    public <T> int update2(Class<T> clazz, String updateSqlSeg, List<?> values) {
        return this.update(clazz, updateSqlSeg, buildArgs(values));
    }

    /**
     * update sql ,返回更新的记录数量
     *
     * @param t   update的对象
     * @param <T> 对象泛型
     * @return 1:表示更新数据成功 0:更新失败(id不存在)
     */
    public <T> int update(T t) {
        return this._update(t, null, false);
    }

    /**
     * cas update 操作
     *
     * @param t   更新的对象
     * @param <T> 对象泛型
     * @return 1:表示更新数据成功 0:更新失败(id不存在或者cas更新失效)
     */
    public <T> int casUpdate(T t) {
        return this._update(t, null, true);
    }

    /**
     * 对实体进行update操作，更新是比较快照与当前实体的值，如果当前实体的值发生变化，才进行更新。
     *
     * @param t        更新的对象
     * @param snapshot 可为空，如果为空就执行 所有字段update
     * @param <T>      对象泛型
     * @return 1:表示更新数据成功 0:更新失败(id不存在)
     */
    public <T> int update(T t, T snapshot) {
        return this._update(t, snapshot, false);
    }

    /**
     * 对实体进行cas update操作，更新是比较快照与当前实体的值，如果当前实体的值发生变化，才进行更新，cas 值发生变化也会更新。
     *
     * @param t        更新的对象
     * @param snapshot 可为空，如果为空就执行 所有字段update
     * @param <T>      对象泛型
     * @return 1:表示更新数据成功 0:更新失败(id不存在或者cas更新失效)
     */
    public <T> int casUpdate(T t, T snapshot) {
        return this._update(t, snapshot, true);
    }

    private <T> int _update(T t, T snapshot, boolean cas) {
        UpdateSnapshotInfo updateSnapshotInfo = SqlBuilder.buildUpdateSegSQLForSnapshot(t, snapshot, cas);
        if (updateSnapshotInfo == null) {
            DALStatus.processDALConClose();
            return 0;
        }
        boolean rollback = false;
        try {
            int result = this.update2(t.getClass(), updateSnapshotInfo.getSqlSeg(), updateSnapshotInfo.getValues());
            if (result == 1) {
                return result;
            }
            rollback = true;
            if (result == 0) {
                return result;
            }
            throw new RuntimeException("update Object must has result=1 or 0,but result[" + result + "]");
        } catch (DataAccessException e) {
            rollback = true;
            throw e;
        } finally {
            if (rollback) {
                EntityTableInfo<T> entityTableInfo = getEntityTableInfo(t.getClass());
                if (cas) {
                    entityTableInfo.setCasFieldValue(t, entityTableInfo.getCasField(), false);
                }
            }
        }
    }

    public void setJdbcSupport(JdbcSupport jdbcSupport) {
        this.jdbcSupport = jdbcSupport;
    }

    public static <T> EntityTableInfo<T> getEntityTableInfo(Class<?> clazz) {
        return (EntityTableInfo<T>) EntityTableInfoFactory.getEntityTableInfo(clazz);
    }

    public JdbcSupport getJdbcSupport() {
        return jdbcSupport;
    }

    public static <T> RowMapper<T> getRowMapper(Class<T> clazz) {
        return (RowMapper<T>) getEntityTableInfo(clazz).getRowMapper();
    }

    public static <T> SQLMapper<T> getSqlMapper(Class<?> clazz) {
        return (SQLMapper<T>) getEntityTableInfo(clazz).getSqlMapper();
    }

    public static Object[] buildArgs(List<?> values) {
        if (values == null) {
            return null;
        }
        Object[] args = new Object[values.size()];
        int i = 0;
        for (Object value : values) {
            args[i] = value;
            i++;
        }
        return args;
    }
    // for distriute

    /**
     * 设置分区解析需要的各种参数
     *
     * @param dalContext 分区context
     */
    private void processDALContext(DALContext dalContext) {
        if (dalContext != null) {
            if (dalContext.isEnableSlave()) {
                DALStatus.setSlaveMode();
                if (dalContext.isSetSlaveDsKey()) {
                    DALStatus.setSlaveDsKey(dalContext.getSlaveDsKey());
                }
            }
            if (!dalContext.isParamMapEmpty()) {
                DALStatus.addParamMap(dalContext.getParamMap());
            }
            if (dalContext.getDalInfo() != null) {
                DALStatus.setDalInfo(dalContext.getDalInfo());
            }
        }
    }

    /**
     * select count(*) 查询。查询中的表别名必须与表名相同
     *
     * @param clazzes    查询对象类型数组
     * @param afterFrom  from table 之后的sql,例如select * from table where uid=?
     *                   order name desc, afterFrom为where uid=? order name desc
     * @param values     参数化查询值
     * @param dalContext 分区context
     * @return sql统计数字
     */
    public int count(Class<?>[] clazzes, String afterFrom, Object[] values,
                     DALContext dalContext) {
        this.processDALContext(dalContext);
        return this.count(clazzes, afterFrom, values);
    }

    /**
     * @param clazzes    查询对象类型数组
     * @param afterFrom  from table 之后的sql,例如select * from table where uid=?
     *                   order name desc, afterFrom为where uid=? order name desc
     * @param values     参数化查询值集合
     * @param dalContext 分区context
     * @return 查询数量
     */
    public int count(Class<?>[] clazzes, String afterFrom, List<?> values, DALContext dalContext) {
        this.processDALContext(dalContext);
        return this.count(clazzes, afterFrom, values);
    }

    /**
     * select count(*) 查询
     *
     * @param clazz      查询对象类型
     * @param afterFrom  from table 之后的sql,例如select * from table where uid=?
     *                   order name desc, afterFrom为where uid=? order name desc
     * @param values     参数化查询值
     * @param dalContext 分区context
     * @param <T>        对象泛型
     * @return 查询数量
     */
    public <T> int count(Class<T> clazz, String afterFrom, Object[] values, DALContext dalContext) {
        this.processDALContext(dalContext);
        return this.count(clazz, afterFrom, values);
    }

    /**
     * select count(*) 查询
     *
     * @param clazz      查询对象类型
     * @param afterFrom  from table 之后的sql,例如select * from table where uid=?
     *                   order name desc, afterFrom为where uid=? order name desc
     * @param values     参数化查询值集合
     * @param dalContext 分区context
     * @param <T>        对象泛型
     * @return 查询数量
     */
    public <T> int count2(Class<T> clazz, String afterFrom, List<?> values, DALContext dalContext) {
        this.processDALContext(dalContext);
        return this.count2(clazz, afterFrom, values);
    }

    /**
     * 对sql中有 in (?,?)的count封装，目前只支持 单个in
     *
     * @param clazz      操作的类
     * @param afterFrom  from之后的sql，例如 where col=?,但是不包括 inColumn
     * @param inColumn   进行in sql操作的列
     * @param values     ?替换符对应的参数，不包括inColumn的参数
     * @param inValues   inColumn对应的参数
     * @param dalContext 分区context
     * @param <T>        集合中对象泛型
     * @return 查询数量
     */
    public <T> int countInValues(Class<T> clazz, String afterFrom, String inColumn, Object[] values, Object[] inValues, DALContext dalContext) {
        this.processDALContext(dalContext);
        return this.countInValues(clazz, afterFrom, inColumn, values, inValues);
    }

    /**
     * 对sql中有 in (?,?)的count封装，目前只支持 单个in
     *
     * @param clazz      操作的类
     * @param afterFrom  from之后的sql，例如 where col=?,但是不包括 inColumn
     * @param inColumn   进行in sql操作的列
     * @param values     ?替换符对应的参数，不包括inColumn的参数
     * @param inValues   inColumn对应的参数
     * @param dalContext 分区context
     * @param <T>        集合中对象泛型
     * @return 查询数量
     */
    public <T> int countInValues2(Class<T> clazz, String afterFrom, String inColumn, List<?> values, List<?> inValues, DALContext dalContext) {
        this.processDALContext(dalContext);
        return this.countInValues2(clazz, afterFrom, inColumn, values, inValues);
    }

    /**
     * sql select
     *
     * @param clazz      查询结果类型
     * @param afterFrom  from之后的sql，例如 where col=? order by uid desc,
     * @param values     参数化查询值
     * @param rowMapper  spring {@link RowMapper} 对象
     * @param dalContext 分区context
     * @param <T>        泛型
     * @return 查询结果 T 类型的集合
     */
    public <T> List<T> list(Class<T> clazz, String afterFrom, Object[] values, RowMapper<T> rowMapper, DALContext dalContext) {
        this.processDALContext(dalContext);
        return this.list(clazz, afterFrom, values, rowMapper);
    }

    /**
     * sql select
     *
     * @param clazz      查询结果类型
     * @param afterFrom  from之后的sql，例如 where col=? order by uid desc,
     * @param values     参数化查询值集合
     * @param rowMapper  spring {@link RowMapper} 对象
     * @param dalContext 分区context
     * @param <T>        泛型
     * @return 查询结果 T 类型的集合
     */
    public <T> List<T> list2(Class<T> clazz, String afterFrom, List<?> values, RowMapper<T> rowMapper, DALContext dalContext) {
        this.processDALContext(dalContext);
        return this.list2(clazz, afterFrom, values, rowMapper);
    }

    /**
     * sql select
     *
     * @param clazz      查询结果类型
     * @param afterFrom  from之后的sql，例如 where col=? order by uid desc,
     * @param values     参数化查询值
     * @param dalContext 分区context
     * @param <T>        泛型
     * @return 查询结果 T 类型的集合
     */
    public <T> List<T> list(Class<T> clazz, String afterFrom, Object[] values, DALContext dalContext) {
        this.processDALContext(dalContext);
        return this.list(clazz, afterFrom, values);
    }

    /**
     * sql select
     *
     * @param clazz      查询结果类型
     * @param afterFrom  from之后的sql，例如 where col=? order by uid desc,
     * @param values     参数化查询值集合
     * @param dalContext 分区context
     * @param <T>        泛型
     * @return 查询结果 T 类型的集合
     */
    public <T> List<T> list2(Class<T> clazz, String afterFrom, List<?> values, DALContext dalContext) {
        this.processDALContext(dalContext);
        return this.list2(clazz, afterFrom, values);
    }

    /**
     * 使用 column in (?,?)的方式来获得集合数据
     *
     * @param clazz      操作的类
     * @param afterFrom  from之后的sql，例如 where col=?,但是不包括 inColumn
     * @param inColumn   进行in sql操作的列
     * @param values     ?替换符对应的参数，不包括inColumn的参数
     * @param inValues   inColumn对应的参数
     * @param dalContext 分区context
     * @param <T>        集合中对象泛型
     * @return 查询结果 T 类型的集合
     */
    public <T> List<T> listInValues(Class<T> clazz, String afterFrom, String inColumn, Object[] values, Object[] inValues, DALContext dalContext) {
        this.processDALContext(dalContext);
        return this.listInValues(clazz, afterFrom, inColumn, values, inValues);
    }

    public <T> List<T> listInValues2(Class<T> clazz, String afterFrom, String inColumn, String afterWhere, List<?> values, List<?> inValues, DALContext dalContext) {
        this.processDALContext(dalContext);
        return this.listInValues2(clazz, afterFrom, inColumn, afterWhere, values, inValues);
    }

    /**
     * 使用 column in (?,?)的方式来获得集合数据
     *
     * @param clazz      操作的类
     * @param afterFrom  from之后的sql，例如 where col=?,但是不包括 inColumn
     * @param inColumn   进行in sql操作的列
     * @param afterWhere 条件语句之后的例如 order by, group by 等语句
     * @param values     ?替换符对应的参数，不包括inColumn的参数
     * @param inValues   inColumn对应的参数
     * @param dalContext 分区context
     * @param <T>        集合中对象泛型
     * @return 查询结果 T 类型的集合
     */
    public <T> List<T> listInValues(Class<T> clazz, String afterFrom, String inColumn, String afterWhere, Object[] values, Object[] inValues, DALContext dalContext) {
        this.processDALContext(dalContext);
        return this.listInValues(clazz, afterFrom, inColumn, afterWhere, values, inValues);
    }

    /**
     * 使用 column in (?,?)的方式来获得集合数据
     *
     * @param clazz      操作的类
     * @param afterFrom  from之后的sql，例如 where col=?,但是不包括 inColumn
     * @param inColumn   进行in sql操作的列
     * @param values     ?替换符对应的参数，不包括inColumn的参数
     * @param inValues   inColumn对应的参数
     * @param dalContext 分区context
     * @param <T>        集合中对象泛型
     * @return 查询结果 T 类型的集合
     */
    public <T> List<T> listInValues2(Class<T> clazz, String afterFrom, String inColumn, List<?> values, List<?> inValues, DALContext dalContext) {
        this.processDALContext(dalContext);
        return this.listInValues2(clazz, afterFrom, inColumn, values, inValues);
    }

    /**
     * @param clazz      操作的类
     * @param afterFrom  from之后的sql，例如 where col=?,但是不包括 inColumn
     * @param inColumn   进行in sql操作的列
     * @param values     ?替换符对应的参数，不包括inColumn的参数
     * @param inValues   inColumn对应的参数
     * @param dalContext 分区context
     * @param <E>        map中key的类型
     * @param <T>        集合中对象泛型
     * @return map对象
     */
    public <E, T> Map<E, T> map(Class<T> clazz, String afterFrom, String inColumn, Object[] values, Object[] inValues, DALContext dalContext) {
        this.processDALContext(dalContext);
        return this.map(clazz, afterFrom, inColumn, values, inValues);
    }

    /**
     * @param clazz      操作的类
     * @param afterFrom  from之后的sql，例如 where col=?,但是不包括 inColumn
     * @param inColumn   进行in sql操作的列
     * @param values     ?替换符对应的参数，不包括inColumn的参数
     * @param inValues   incolumn对应的参数
     * @param dalContext 分区context
     * @param <E>        map中key的类型
     * @param <T>        集合中对象泛型
     * @return map对象
     */
    public <E, T> Map<E, T> map2(Class<T> clazz, String afterFrom, String inColumn, List<?> values, List<?> inValues, DALContext dalContext) {
        this.processDALContext(dalContext);
        return this.map2(clazz, afterFrom, inColumn, values, inValues);
    }

    /**
     * delete sql.根据条件删除.例如: delete table where field0=? and ....
     *
     * @param clazz      要删除的对象类型
     * @param afterFrom  delete table 之后的语句,例如:delete table where field0=?,afterFrom为where field0=?
     * @param values     参数化查询值
     * @param dalContext 分区context
     * @param <T>        对象泛型
     * @return 删除的记录数
     */
    public <T> int delete(Class<T> clazz, String afterFrom, Object[] values, DALContext dalContext) {
        this.processDALContext(dalContext);
        return this.delete(clazz, afterFrom, values);
    }

    /**
     * 批量删除
     *
     * @param clazz      要删除的对象
     * @param afterFrom  delete table 之后的语句,例如:delete table where field0=?,afterFrom为where field0=?
     * @param valuesList 批量操作的参数集合
     * @param dalContext 分区context
     * @param <T>        类泛型
     * @return delete result
     */
    public <T> int[] batchDelete(Class<T> clazz, String afterFrom, List<Object[]> valuesList, DALContext dalContext) {
        this.processDALContext(dalContext);
        return this.batchDelete(clazz, afterFrom, valuesList);
    }

    /**
     * 删除
     *
     * @param clazz      要删除的对象
     * @param afterFrom  delete table 之后的语句,例如:delete table where field0=?,afterFrom为where field0=?
     * @param values     参数
     * @param dalContext 分区context
     * @param <T>        类泛型
     * @return delete result
     */
    public <T> int delete2(Class<T> clazz, String afterFrom, List<?> values, DALContext dalContext) {
        this.processDALContext(dalContext);
        return this.delete2(clazz, afterFrom, values);
    }

    /**
     * delete sql,返回删除的记录数量
     *
     * @param t          要删除的对象，必须有id
     * @param dalContext 分区context
     * @param <T>        对象泛型
     * @return 删除的记录数
     */
    public <T> int delete(T t, DALContext dalContext) {
        this.processDALContext(dalContext);
        return this.delete(t);
    }

    /**
     * delete sql,根据id删除。返回删除的记录数量
     *
     * @param clazz      要删除的对象的类型
     * @param idValues   主键id值
     * @param dalContext 分区context
     * @param <T>        对象泛型
     * @return sql操作失败的异常
     */
    public <T> int deleteById(Class<T> clazz, Object[] idValues, DALContext dalContext) {
        this.processDALContext(dalContext);
        return this.deleteById(clazz, idValues);
    }

    /**
     * 批量insert,对于使用数据库自增id方式，不会返回自增id，请使用应用自行获得自增id
     *
     * @param list       批量床架的对象
     * @param dalContext 分区context
     * @param <T>        对象类型
     * @return 返回自增id，如果id不是自增，就返回值为0的集合
     */
    public <T> List<T> batchInsert(List<T> list, DALContext dalContext) {
        this.processDALContext(dalContext);
        return this.batchInsert(list);
    }

    /**
     * insert sql
     *
     * @param t          insert的对象
     * @param dalContext 分区context
     * @param <T>        对象泛型
     */
    public <T> void insert(T t, DALContext dalContext) {
        this.processDALContext(dalContext);
        this.insert(t);
    }

    /**
     * replace into sql,此操作，如果是更新数据，将不会返回自增id
     *
     * @param t          数据对象
     * @param dalContext 分区context
     * @param <T>        泛型
     * @return 当进行replace操作时，返回0
     */
    public <T> Number replace(T t, DALContext dalContext) {
        this.processDALContext(dalContext);
        return this.replace(t);
    }

    /**
     * insert ignore into sql
     *
     * @param t          数据对象
     * @param dalContext 分区context
     * @param <T>        泛型
     * @return 当插入不成功时，返回0
     */
    public <T> Number insertIgnore(T t, DALContext dalContext) {
        this.processDALContext(dalContext);
        return this.insertIgnore(t);
    }

    /**
     * @param t          数据对象
     * @param insertFlag 操作标识
     * @param dalContext 分区context
     * @param <T>        泛型
     * @return 返回自增id，如果没有自增id，返回0
     */
    public <T> Number insertForNumber(T t, InsertFlag insertFlag, DALContext dalContext) {
        this.processDALContext(dalContext);
        return this.insertForNumber(t, insertFlag);
    }

    /**
     * insert sql,返回自增数字id，联合主键的表，返回0. 如果表没有主键，直接insert,返回0
     *
     * @param t          insert的对象
     * @param dalContext 分区context
     * @param <T>        对象泛型
     * @return insert之后的自增数字
     */
    public <T> Number insertForNumber(T t, DALContext dalContext) {
        this.processDALContext(dalContext);
        return this.insertForNumber(t);
    }

    /**
     * mysql的分页查询。查询中的表别名必须与表名相同
     *
     * @param clazzes    查询对象类型数组
     * @param afterFrom  from table 之后的sql,例如select * from table where uid=?
     *                   order name desc, afterFrom为where uid=? order name
     * @param begin      开始位置
     * @param size       查询数量
     * @param values     参数化查询值
     * @param rowMapper  spring RowMapper
     * @param dalContext 分区context
     * @param <T>        对象泛型
     * @return 查询结果 T 类型的集合
     */
    public <T> List<T> mysqlList(Class<?>[] clazzes, String afterFrom, int begin, int size, Object[] values, RowMapper<T> rowMapper, DALContext dalContext) {
        this.processDALContext(dalContext);
        return this.mysqlList(clazzes, afterFrom, begin, size, values, rowMapper);
    }

    /**
     * mysql的分页查询。查询中的表别名必须与表名相同
     *
     * @param clazzes    查询对象类型数组
     * @param afterFrom  from table 之后的sql,例如select * from table where uid=?
     *                   order name desc, afterFrom为where uid=? order name
     * @param begin      开始位置
     * @param size       查询数量
     * @param values     参数化查询值集合
     * @param rowMapper  spring RowMapper
     * @param dalContext 分区context
     * @param <T>        对象泛型
     * @return 查询结果 T 类型的集合
     */
    public <T> List<T> mysqlList2(Class<?>[] clazzes, String afterFrom, int begin, int size, List<?> values, RowMapper<T> rowMapper, DALContext dalContext) {
        this.processDALContext(dalContext);
        return this.mysqlList2(clazzes, afterFrom, begin, size, values, rowMapper);
    }

    /**
     * mysql的分页查询。
     *
     * @param clazz      查询对象类型
     * @param afterFrom  from table 之后的sql,例如select * from table where uid=?
     *                   order name desc, afterFrom为where uid=? order name
     * @param begin      开始位置
     * @param size       查询数量
     * @param values     参数化查询值
     * @param dalContext 分区context
     * @param <T>        对象泛型
     * @return 查询结果 T 类型的集合
     */
    public <T> List<T> mysqlList(Class<T> clazz, String afterFrom, int begin, int size, Object[] values, DALContext dalContext) {
        this.processDALContext(dalContext);
        return this.mysqlList(clazz, afterFrom, begin, size, values);
    }

    /**
     * mysql的分页查询。
     *
     * @param clazz      查询对象类型
     * @param afterFrom  from table 之后的sql,例如select * from table where uid=?
     *                   order name desc, afterFrom为where uid=? order name
     * @param begin      开始位置
     * @param size       查询数量集合
     * @param values     参数化查询值
     * @param dalContext 分区context
     * @param <T>        对象泛型
     * @return 查询结果 T 类型的集合
     */
    public <T> List<T> mysqlList2(Class<T> clazz, String afterFrom, int begin, int size, List<?> values, DALContext dalContext) {
        this.processDALContext(dalContext);
        return this.mysqlList2(clazz, afterFrom, begin, size, values);
    }

    /**
     * mysql的分页查询。
     *
     * @param clazz      查询对象类型
     * @param afterFrom  from table 之后的sql,例如select * from table where uid=?
     *                   order name desc, afterFrom为where uid=? order name
     * @param begin      开始位置
     * @param size       查询数量
     * @param values     参数化查询值
     * @param rowMapper  spring RowMapper
     * @param dalContext 分区context
     * @param <T>        对象泛型
     * @return 查询结果 T 类型的集合
     */
    public <T> List<T> mysqlList(Class<T> clazz, String afterFrom, int begin, int size, Object[] values, RowMapper<T> rowMapper, DALContext dalContext) {
        this.processDALContext(dalContext);
        return this.mysqlList(clazz, afterFrom, begin, size, values, rowMapper);
    }

    /**
     * mysql的分页查询。
     *
     * @param clazz      查询对象类型
     * @param afterFrom  from table 之后的sql,例如select * from table where uid=?
     *                   order name desc, afterFrom为where uid=? order name
     * @param begin      开始位置
     * @param size       查询数量
     * @param values     参数化查询值集合
     * @param rowMapper  spring RowMapper
     * @param dalContext 分区context
     * @param <T>        对象泛型
     * @return 查询结果 T 类型的集合
     */
    public <T> List<T> mysqlList2(Class<T> clazz, String afterFrom, int begin, int size, List<?> values, RowMapper<T> rowMapper, DALContext dalContext) {
        this.processDALContext(dalContext);
        return this.mysqlList2(clazz, afterFrom, begin, size, values, rowMapper);
    }

    /**
     * select sql 返回对象
     *
     * @param clazz      查询对象类型
     * @param afterFrom  from table 之后的sql,例如select * from table where uid=?
     *                   order name desc, afterFrom为where uid=? order name desc
     * @param values     参数化查询值
     * @param dalContext 分区context
     * @param <T>        对象泛型
     * @return 查询 T 类型对象，null表示没有搜索结果
     */
    public <T> T obj(Class<T> clazz, String afterFrom, Object[] values, DALContext dalContext) {
        this.processDALContext(dalContext);
        return this.obj(clazz, afterFrom, values);
    }

    /**
     * select sql 返回对象
     *
     * @param clazz      查询对象类型
     * @param afterFrom  from table 之后的sql,例如select * from table where uid=?
     *                   order name desc, afterFrom为where uid=? order name desc
     * @param values     参数化查询值集合
     * @param dalContext 分区context
     * @param <T>        对象泛型
     * @return 查询 T 类型对象，null表示没有搜索结果
     */
    public <T> T obj2(Class<T> clazz, String afterFrom, List<?> values, DALContext dalContext) {
        this.processDALContext(dalContext);
        return this.obj2(clazz, afterFrom, values);
    }

    /**
     * select sql 返回对象
     *
     * @param clazz      查询对象类型
     * @param afterFrom  from table 之后的sql,例如select * from table where uid=?
     *                   order name desc, afterFrom为where uid=? order name desc
     * @param values     参数化查询值
     * @param rowMapper  spring RowMapper
     * @param dalContext 分区context
     * @param <T>        对象泛型
     * @return 查询 T 类型对象，null表示没有搜索结果
     */
    public <T> T obj(Class<T> clazz, String afterFrom, Object[] values, RowMapper<T> rowMapper, DALContext dalContext) {
        this.processDALContext(dalContext);
        return this.obj(clazz, afterFrom, values, rowMapper);
    }

    /**
     * select sql 返回对象
     *
     * @param clazz      查询对象类型
     * @param afterFrom  from table 之后的sql,例如select * from table where uid=?
     *                   order name desc, afterFrom为where uid=? order name desc
     * @param values     参数化查询值集合
     * @param rowMapper  spring RowMapper
     * @param dalContext 分区context
     * @param <T>        对象泛型
     * @return 查询 T 类型对象，null表示没有搜索结果
     */
    public <T> T obj2(Class<T> clazz, String afterFrom, List<?> values, RowMapper<T> rowMapper, DALContext dalContext) {
        this.processDALContext(dalContext);
        return this.obj2(clazz, afterFrom, values, rowMapper);
    }

    /**
     * select sql 根据id查询，返回对象
     *
     * @param clazz      查询对象类型
     * @param idValue    id参数
     * @param dalContext 分区context
     * @param <T>        对象泛型
     * @return 查询 T 类型对象，null表示没有搜索结果
     */
    public <T> T objById(Class<T> clazz, Object idValue, DALContext dalContext) {
        this.processDALContext(dalContext);
        return this.objById(clazz, idValue);
    }

    /**
     * select sql 根据id查询,并且实现sql select ... for update 功能
     *
     * @param clazz      查询对象类型
     * @param idValue    id参数
     * @param forUpdate  是否锁数据
     * @param dalContext 分区context
     * @param <T>        对象泛型
     * @return 查询 T 类型对象，null表示没有搜索结果
     */
    public <T> T objById(Class<T> clazz, Object idValue, boolean forUpdate, DALContext dalContext) {
        this.processDALContext(dalContext);
        return this.objById(clazz, idValue, forUpdate);
    }

    /**
     * select 根据id查询对象，并使用for update 锁定该行数据
     *
     * @param clazz      查询对象类型
     * @param idValue    id参数
     * @param dalContext 分区context
     * @param <T>        对象泛型
     * @return 查询 T 类型对象，null表示没有搜索结果
     */
    public <T> T objByIdForUpdate(Class<T> clazz, Object idValue, DALContext dalContext) {
        this.processDALContext(dalContext);
        return this.objByIdForUpdate(clazz, idValue);
    }

    /**
     * select sql 根据id查询，返回对象
     *
     * @param clazz      查询对象类型
     * @param idValues   id参数
     * @param dalContext 分区context
     * @param <T>        对象泛型
     * @return 查询 T 类型对象，null表示没有搜索结果
     */
    public <T> T objByIds(Class<T> clazz, Object[] idValues, DALContext dalContext) {
        this.processDALContext(dalContext);
        return this.objByIds(clazz, idValues);
    }

    /**
     * select 根据id查询对象,并使用for update 锁定该行数据
     *
     * @param clazz      查询对象类型
     * @param idValues   id参数
     * @param dalContext 分区context
     * @param <T>        对象泛型
     * @return 查询 T 类型对象，null表示没有搜索结果
     */
    public <T> T objByIdsForUpdate(Class<T> clazz, Object[] idValues, DALContext dalContext) {
        this.processDALContext(dalContext);
        return this.objByIdsForUpdate(clazz, idValues);
    }

    /**
     * select 根据id查询对象,并使用for update 锁定该行数据
     *
     * @param clazz      查询对象类型
     * @param idValues   id参数
     * @param forUpdate  是否锁对象
     * @param dalContext 分区context
     * @param <T>        对象泛型
     * @return 查询 T 类型对象，null表示没有搜索结果
     */
    public <T> T objByIds(Class<T> clazz, Object[] idValues, boolean forUpdate, DALContext dalContext) {
        this.processDALContext(dalContext);
        return this.objByIds(clazz, idValues, forUpdate);
    }

    /**
     * select sql 根据id查询，返回对象
     *
     * @param clazz      查询对象类型
     * @param idValues   id参数
     * @param forUpdate  是否使用sql for update进行锁数据
     * @param rowMapper  spring RowMapper
     * @param dalContext 分区context
     * @param <T>        对象泛型
     * @return 查询 T 类型对象，null表示没有搜索结果
     */
    public <T> T objByIds(Class<T> clazz, Object[] idValues, boolean forUpdate, RowMapper<T> rowMapper, DALContext dalContext) {
        this.processDALContext(dalContext);
        return this.objByIds(clazz, idValues, forUpdate, rowMapper);
    }

    /**
     * 批量更新
     *
     * @param clazz        更新的类型
     * @param updateSqlSeg sql片段,为update table 之后的sql。例如：set field0=?,field1=? where field3=?
     * @param valuesList   批量操作的参数集合
     * @param dalContext   分区context
     * @param <T>          类泛型
     * @return update result
     */
    public <T> int[] batchUpdate(Class<T> clazz, String updateSqlSeg, List<Object[]> valuesList, DALContext dalContext) {
        this.processDALContext(dalContext);
        return this.batchUpdate(clazz, updateSqlSeg, valuesList);
    }

    /**
     * update sql，返回更新的记录数量。只更新选中的字段 例如: update table set field0=?,field1=?
     * where field3=?
     *
     * @param clazz        需要更新的类
     * @param updateSqlSeg sql片段,为update table 之后的sql。例如：set field0=?,field1=? where field3=?
     * @param values       参数化查询值
     * @param dalContext   分区context
     * @param <T>          对象泛型
     * @return 更新数量
     */
    public <T> int update(Class<T> clazz, String updateSqlSeg, Object[] values, DALContext dalContext) {
        this.processDALContext(dalContext);
        return this.update(clazz, updateSqlSeg, values);
    }

    /**
     * update sql，返回更新的记录数量。只更新选中的字段 例如: update table set field0=?,field1=?
     *
     * @param clazz        需要更新的类
     * @param updateSqlSeg sql片段,为update table 之后的sql。例如：set field0=?,field1=? where field3=?
     * @param values       参数化查询值
     * @param dalContext   分区context
     * @param <T>          类泛型
     * @return 更新数量
     */
    public <T> int update2(Class<T> clazz, String updateSqlSeg, List<?> values, DALContext dalContext) {
        this.processDALContext(dalContext);
        return this.update2(clazz, updateSqlSeg, values);
    }

    /**
     * update sql ,返回更新的记录数量
     *
     * @param t          update的对象
     * @param dalContext 分区context
     * @param <T>        对象泛型
     * @return 更新数量
     */
    public <T> int update(T t, DALContext dalContext) {
        this.processDALContext(dalContext);
        return this.update(t);
    }

    /**
     * 对实体进行update操作，更新是比较快照与当前实体的值，如果当前实体的值发生变化，才进行更新。
     *
     * @param t          要更新的对象
     * @param snapshot   可为空，如果为空就执行 update(T t)
     * @param dalContext 分区context
     * @param <T>        泛型
     * @return update result
     */
    public <T> int update(T t, T snapshot, DALContext dalContext) {
        this.processDALContext(dalContext);
        return this.update(t, snapshot);
    }

    /**
     * 对实体对象进行属性快照，记录当前实体中filed 的值到新的对象中
     *
     * @param t   实体对象
     * @param <T> 泛型
     * @return 对象快照
     */
    public static <T> T snapshot(Object t) {
        EntityTableInfo<T> entityTableInfo = Query.getEntityTableInfo(t.getClass());
        try {
            T snapshoot = entityTableInfo.getConstructor().newInstance();
            EntityUtil.copy(t, snapshoot);
            return snapshoot;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 请使用 {@link DALParserUtil#process(Class, DALParser)}
     *
     * @param clazz     需要分区的对象class
     * @param dalParser 解析器对象
     * @return 分区信息
     */
    @Deprecated
    public static DALInfo process(Class clazz, DALParser dalParser) {
        return DALParserUtil.process(clazz, dalParser);
    }

    /**
     * 请使用 {@link DALParserUtil#process(Class)}
     *
     * @param clazz 需要分区的对象class
     * @param <T>   对象泛型
     * @return 分区信息
     */
    @Deprecated
    public static <T> DALInfo process(Class<T> clazz) {
        EntityTableInfo<T> entityTableInfo = getEntityTableInfo(clazz);
        return process(entityTableInfo.getClazz(), entityTableInfo.getDalParser());
    }

}