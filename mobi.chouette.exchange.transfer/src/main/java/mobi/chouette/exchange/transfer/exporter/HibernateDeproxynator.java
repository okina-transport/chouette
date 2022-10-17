package mobi.chouette.exchange.transfer.exporter;

import lombok.extern.log4j.Log4j;
import mobi.chouette.model.NeptuneObject;
import org.apache.commons.beanutils.PropertyUtils;
import org.hibernate.Hibernate;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.LazyInitializer;

import javax.persistence.Embeddable;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Log4j
public class HibernateDeproxynator<T> {
	public List deepDeproxy(final List maybeProxy) throws ClassCastException {
		if (maybeProxy == null)
			return null;
		HashSet<Object> visited = new HashSet<>(1000000);
		HashSet<Object> moreObjectsToFollow = new HashSet<>(1000000);

		List<T> results = new ArrayList<>(maybeProxy.size());
		for (Object x : maybeProxy) {
			T ret = deepDeproxy(x, visited, moreObjectsToFollow);
			results.add(ret);
		}

		boolean finished = false;
		while (!finished) {
			HashSet<Object> newObjectsToFollow = new HashSet<>(1000000);
			log.info("Objects to follow initial queue size=" + moreObjectsToFollow.size());
			Iterator<Object> it = moreObjectsToFollow.iterator();
			while (it.hasNext()) {
				Object next = it.next();
				if (!visited.contains(next)) {
					deepDeproxy(next, visited, newObjectsToFollow);
				}
			}

			moreObjectsToFollow.clear();
			log.info("Adding " + newObjectsToFollow.size() + " more objects to follow");
			moreObjectsToFollow = newObjectsToFollow;
			it = moreObjectsToFollow.iterator();
			finished = !it.hasNext();
		}

		// Ease garbage collection
		visited.clear();
		moreObjectsToFollow.clear();

		return results;
	}

	private T deepDeproxy(final Object maybeProxy, final Set<Object> visited, HashSet<Object> moreObjectsToFollow)
			throws ClassCastException {
		if (maybeProxy == null)
			return null;
		Class clazz;
		Hibernate.initialize(maybeProxy);
		if (maybeProxy instanceof HibernateProxy) {
			HibernateProxy proxy = (HibernateProxy) maybeProxy;
			LazyInitializer li = proxy.getHibernateLazyInitializer();
			clazz = li.getImplementation().getClass();
		} else {
			clazz = maybeProxy.getClass();
		}

		T ret = (T) deepDeproxy(maybeProxy, clazz);
		boolean deproxyAgain = false;
		if (visited.contains(ret)) {
			if(isReferentialObject(ret)) {
				NeptuneObject rs = (NeptuneObject) ret;
				if(rs.getId() != null) {
					deproxyAgain = true;
				}
			}
			if(!deproxyAgain) {
				return ret;
			}
		}
		visited.add(ret);

		if (isReferentialObject(ret)) {
			((NeptuneObject) ret).setId(null);
			((NeptuneObject) ret).setDetached(true);

		}

		if (ret instanceof Object[] || ret instanceof Set || ret instanceof Map || ret instanceof List) {
			// Deproxy elements of collection
			if (ret instanceof Object[]) {
				Object[] valueArray = (Object[]) ret;
				Arrays.setAll(valueArray, i -> deepDeproxy(valueArray[i], visited, moreObjectsToFollow));
			} else if (ret instanceof Set) {
				Set<?> valueSet = (Set) ret;
				Collection result = valueSet.stream().map(o -> deepDeproxy(o, visited, moreObjectsToFollow)).collect(Collectors.toList());
				valueSet.clear();
				valueSet.addAll(result);
			} else if (ret instanceof Map) {
				Map<?,?> valueMap = (Map) ret;
				Map result = valueMap.entrySet().stream().collect(Collectors.toMap(entry -> deepDeproxy(entry.getKey(), visited, moreObjectsToFollow), entry -> deepDeproxy(entry.getValue(), visited, moreObjectsToFollow)));
				valueMap.clear();
				valueMap.putAll(result);
			} else if (ret instanceof List) {
                List valueList = (List) ret;
                // Deep de-proxying of entities in collection may alter the order of the collection it self because of
				// back-references. Working on copies and replacing org collection afterwards
                List orgList = new ArrayList(valueList);
                List deProxiedValues = new ArrayList(valueList.size());
                for (int i = 0; i < orgList.size(); i++) {
                    T proxY = deepDeproxy(orgList.get(i), visited, moreObjectsToFollow);
                    if (i >= orgList.size()) {
                        log.warn("Would have outbounded array: " + proxY + " for " + maybeProxy);
                    } else {
                        deProxiedValues.add(proxY);
                    }
                }
                valueList.clear();
                valueList.addAll(deProxiedValues);
            }
		} else {

			for (PropertyDescriptor property : PropertyUtils.getPropertyDescriptors(ret)) {
				try {
					String name = property.getName();
					if (!"owner".equals(name) && property.getWriteMethod() != null) {
						Object value = PropertyUtils.getProperty(ret, name);

						boolean needToSetProperty = false;
						if (value instanceof HibernateProxy) {
							value = deepDeproxy(value, visited, moreObjectsToFollow);
							needToSetProperty = true;
						}

						if (value instanceof Object[]) {
							Object[] valueArray = (Object[]) value;
							Object[] result = (Object[]) Array.newInstance(value.getClass(), valueArray.length);
							System.arraycopy(valueArray, 0, result, 0, valueArray.length);
							value = result;
							needToSetProperty = true;
							if (result.length > 0) {
								moreObjectsToFollow.add(result);
							}
						} else if (value instanceof Set) {
							Set valueSet = (Set) value;
							Set result = new HashSet(valueSet);
							value = result;
							needToSetProperty = true;
							valueSet.clear();
							if (!result.isEmpty()) {
								moreObjectsToFollow.add(result);
							}
						} else if (value instanceof Map) {
							Map valueMap = (Map) value;
							Map result = new HashMap(valueMap);
							value = result;
							needToSetProperty = true;
							valueMap.clear();
							if (!result.isEmpty()) {
								moreObjectsToFollow.add(result);
							}
						} else if (value instanceof List) {
							List valueList = (List) value;
							List result = new ArrayList(valueList);
							value = result;
							needToSetProperty = true;
							valueList.clear();
							if (!result.isEmpty()) {
								moreObjectsToFollow.add(result);
							}
						}
						if (needToSetProperty) {
							PropertyUtils.setProperty(ret, name, value);
						}
						if (isReferentialObject(value) || isEmbeddable(value)) {
							// Follow any Neptune data relations to discover
							// more
							// proxies
							// log.info("Delaying following of object
							// "+value.getClass().getSimpleName());
							moreObjectsToFollow.add(value);
						}
					}
				} catch (java.lang.IllegalAccessException e) {
					e.printStackTrace();
				} catch (InvocationTargetException e) {
					e.printStackTrace();
				} catch (NoSuchMethodException e) {
					e.printStackTrace();
				}
			}

		}

		return ret;
	}

	private boolean isReferentialObject(Object ret) {
		return ret instanceof NeptuneObject;
	}

	private boolean isEmbeddable(Object ret){
		return ret!=null && ret.getClass().getAnnotation(Embeddable.class) != null;
	}

	private <T> T deepDeproxy(Object maybeProxy, Class<T> baseClass) throws ClassCastException {
		return baseClass.cast(Hibernate.unproxy(maybeProxy));
	}

}
