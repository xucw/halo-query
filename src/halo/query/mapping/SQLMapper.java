package halo.query.mapping;

/**
 * 此接口的存在是为了利用字节码进行类生成，提高性能，避免使用反射操作
 * 
 * @author akwei
 * @param <T>
 */
public interface SQLMapper<T> {

	/**
	 * 返回insert需要的参数
	 * 
	 * @param t 需要insert的对象
	 * @param hasIdFieldValue 是否包含id的值
	 * @return
	 */
	Object[] getParamsForInsert(T t, boolean hasIdFieldValue);

	/**
	 * 返回update需要的参数以及id所对应的参数组成的数组
	 * 
	 * @param t 需要update的对象
	 * @return
	 */
	Object[] getParamsForUpdate(T t);

	/**
	 * 返回delete by id 时id的参数
	 * 
	 * @param t 要删除的对象
	 * @return
	 */
	Object getIdParam(T t);
}
