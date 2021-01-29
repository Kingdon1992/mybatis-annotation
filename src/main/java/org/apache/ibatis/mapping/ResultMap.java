/**
 *    Copyright 2009-2019 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.apache.ibatis.mapping;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.builder.BuilderException;
import org.apache.ibatis.logging.Log;
import org.apache.ibatis.logging.LogFactory;
import org.apache.ibatis.reflection.ParamNameUtil;
import org.apache.ibatis.session.Configuration;

/**
 * @author Clinton Begin
 */
public class ResultMap {
  private Configuration configuration;
  /**
   * 该ResultMap对象的id，以所属名称空间开头
   */
  private String id;
  /**
   * 结果类
   */
  private Class<?> type;
  /**
   * 所有的ResultMapping对象，后续会被分类存入
   * idResultMappings
   * constructorResultMappings
   * propertyResultMappings
   */
  private List<ResultMapping> resultMappings;
  /**
   * 判断resultMappings域是否存在ResultMapping对象含有id标志
   * ① 有：将含有id标志的ResultMapping对象其放入该域
   * ② 无：将resultMappings域持有的所有ResultMapping对象放入该域
   */
  private List<ResultMapping> idResultMappings;
  /**
   * 含有ResultFlag.CONSTRUCTOR标志的ResultMapping对象
   */
  private List<ResultMapping> constructorResultMappings;
  /**
   * 不含有ResultFlag.CONSTRUCTOR标志的ResultMapping对象
   */
  private List<ResultMapping> propertyResultMappings;
  /**
   * 记录了所有参与映射的数据库字段名
   */
  private Set<String> mappedColumns;
  /**
   * 记录了所有参与映射的Java字段名
   */
  private Set<String> mappedProperties;
  private Discriminator discriminator;
  private boolean hasNestedResultMaps;
  private boolean hasNestedQueries;
  private Boolean autoMapping;

  private ResultMap() {
  }

  public static class Builder {
    private static final Log log = LogFactory.getLog(Builder.class);

    private ResultMap resultMap = new ResultMap();

    public Builder(Configuration configuration, String id, Class<?> type, List<ResultMapping> resultMappings) {
      this(configuration, id, type, resultMappings, null);
    }

    public Builder(Configuration configuration, String id, Class<?> type, List<ResultMapping> resultMappings, Boolean autoMapping) {
      resultMap.configuration = configuration;
      resultMap.id = id;
      resultMap.type = type;
      resultMap.resultMappings = resultMappings;
      resultMap.autoMapping = autoMapping;
    }

    public Builder discriminator(Discriminator discriminator) {
      resultMap.discriminator = discriminator;
      return this;
    }

    public Class<?> type() {
      return resultMap.type;
    }

    public ResultMap build() {
      if (resultMap.id == null) {
        throw new IllegalArgumentException("ResultMaps must have an id");
      }
      resultMap.mappedColumns = new HashSet<>();
      resultMap.mappedProperties = new HashSet<>();
      resultMap.idResultMappings = new ArrayList<>();
      resultMap.constructorResultMappings = new ArrayList<>();
      resultMap.propertyResultMappings = new ArrayList<>();
      final List<String> constructorArgNames = new ArrayList<>();
      for (ResultMapping resultMapping : resultMap.resultMappings) {
        // 任意一个ResultMapping对象持有嵌套查询id，证明该ResultMap对象有嵌套查询
        resultMap.hasNestedQueries = resultMap.hasNestedQueries || resultMapping.getNestedQueryId() != null;
        resultMap.hasNestedResultMaps = resultMap.hasNestedResultMaps || (resultMapping.getNestedResultMapId() != null && resultMapping.getResultSet() == null);
        final String column = resultMapping.getColumn();
        if (column != null) {
          resultMap.mappedColumns.add(column.toUpperCase(Locale.ENGLISH));
        } else if (resultMapping.isCompositeResult()) {
          for (ResultMapping compositeResultMapping : resultMapping.getComposites()) {
            final String compositeColumn = compositeResultMapping.getColumn();
            if (compositeColumn != null) {
              resultMap.mappedColumns.add(compositeColumn.toUpperCase(Locale.ENGLISH));
            }
          }
        }
        final String property = resultMapping.getProperty();
        if (property != null) {
          resultMap.mappedProperties.add(property);
        }
        /**
         * 检查到{@link ResultMapping}对象含有构造器标志{@link ResultFlag.CONSTRUCTOR}
         * ① 含有：将该对象加入constructorResultMappings，并做后续处理
         *      - 当该对象property域值不为空时（<arg/>或<idArg/>标签的name属性解析而来），存储该值，后续根据这些值寻找类的构造器
         * ② 不含有：将该对象加入propertyResultMappings
         */
        if (resultMapping.getFlags().contains(ResultFlag.CONSTRUCTOR)) {
          resultMap.constructorResultMappings.add(resultMapping);
          if (resultMapping.getProperty() != null) {
            constructorArgNames.add(resultMapping.getProperty());
          }
        } else {
          resultMap.propertyResultMappings.add(resultMapping);
        }

        // 检查到ResultMapping含有ResultFlag.ID标志，有则将该ResultMapping对象加入到ResultMap对象维护的List<ResultMapping>存储结构中，域名idResultMappings
        if (resultMapping.getFlags().contains(ResultFlag.ID)) {
          resultMap.idResultMappings.add(resultMapping);
        }
      }
      if (resultMap.idResultMappings.isEmpty()) {
        resultMap.idResultMappings.addAll(resultMap.resultMappings);
      }
      /**
       * constructorArgNames结构存储了含有{@link ResultFlag.CONSTRUCTOR}标志{@link ResultMapping}对象的{@link ResultMapping.property}域值，这些值会被用来定位构造器，定位逻辑如下
       * ① 解析<arg/>、<idArg/>标签，取出name属性作为一个数组
       * ② 根据<resultMap/>标签的type属性，找到对应的结果类的Class对象，再根据<arg/>、<idArg/>标签数量，取出形参数量一致的构造方法
       * ③ 选取第一个符合目标的构造器
       *  - 取出构造器所有形参名称（优先从{@link Param}注解中取，如果开启{@link Configuration.useActualParamName}，则通过反射从构造器对象中取，前提是编译参数"-parameters"开启，否则返回默认参数"arg0"这种）
       *  - 与constructorArgNames比较，如果数量一致、含有的入参名称一致、入参名称一样的类型也一致，则命中
       * ④ 根据构造器实际的入参名称顺序，为{@link constructorResultMappings}内部的{@link ResultMapping}对象排序
       */
      if (!constructorArgNames.isEmpty()) {
        final List<String> actualArgNames = argNamesOfMatchingConstructor(constructorArgNames);
        if (actualArgNames == null) {
          throw new BuilderException("Error in result map '" + resultMap.id
              + "'. Failed to find a constructor in '"
              + resultMap.getType().getName() + "' by arg names " + constructorArgNames
              + ". There might be more info in debug log.");
        }
        resultMap.constructorResultMappings.sort((o1, o2) -> {
          int paramIdx1 = actualArgNames.indexOf(o1.getProperty());
          int paramIdx2 = actualArgNames.indexOf(o2.getProperty());
          return paramIdx1 - paramIdx2;
        });
      }
      // lock down collections
      resultMap.resultMappings = Collections.unmodifiableList(resultMap.resultMappings);
      resultMap.idResultMappings = Collections.unmodifiableList(resultMap.idResultMappings);
      resultMap.constructorResultMappings = Collections.unmodifiableList(resultMap.constructorResultMappings);
      resultMap.propertyResultMappings = Collections.unmodifiableList(resultMap.propertyResultMappings);
      resultMap.mappedColumns = Collections.unmodifiableSet(resultMap.mappedColumns);
      return resultMap;
    }

    // 此处的入参是<arg/>、<idArg/>标签的name属性数组
    private List<String> argNamesOfMatchingConstructor(List<String> constructorArgNames) {
      Constructor<?>[] constructors = resultMap.type.getDeclaredConstructors();
      for (Constructor<?> constructor : constructors) {
        Class<?>[] paramTypes = constructor.getParameterTypes();
        // 找到参数数量一致的构造方法
        if (constructorArgNames.size() == paramTypes.length) {
          List<String> paramNames = getArgNames(constructor);
          if (constructorArgNames.containsAll(paramNames)
              && argTypesMatch(constructorArgNames, paramTypes, paramNames)) {
            return paramNames;
          }
        }
      }
      return null;
    }

    /**
     * 找到的构造方法是否匹配
     * @param constructorArgNames <arg/>、<idArg/>标签的name属性集合
     * @param paramTypes 目标构造方法的形参类型集合
     * @param paramNames 目标构造方法的形参名称集合（来源：@param注解、默认值"argx"、源码形参名）
     * @return 是否匹配
     */
    private boolean argTypesMatch(final List<String> constructorArgNames,
        Class<?>[] paramTypes, List<String> paramNames) {
      for (int i = 0; i < constructorArgNames.size(); i++) {
        /*
        因为满足下面两个条件，才能这么玩
        ① paramTypes与paramNames顺序一致
        ② constructorResultMappings与constructorArgNames一致
        也是因为该部分的处理，才能使得即便<arg/>、<idArg/>标签乱序，依然能够识别出对应的构造方法
         */
        Class<?> actualType = paramTypes[paramNames.indexOf(constructorArgNames.get(i))];
        Class<?> specifiedType = resultMap.constructorResultMappings.get(i).getJavaType();
        if (!actualType.equals(specifiedType)) {
          if (log.isDebugEnabled()) {
            log.debug("While building result map '" + resultMap.id
                + "', found a constructor with arg names " + constructorArgNames
                + ", but the type of '" + constructorArgNames.get(i)
                + "' did not match. Specified: [" + specifiedType.getName() + "] Declared: ["
                + actualType.getName() + "]");
          }
          return false;
        }
      }
      return true;
    }

    /**
     * 根据一个类的构造方法获取该方法的所有入参名称，按以下优先级
     * ①注解{@link Param}的value值
     * ②jdk编译后的形参名，默认值还是"arg0"、"arg1"，但是可以通过添加编译参数"-parameters"来使编译时仍然使用源码中定义的形参名
     * ③默认值"arg"+index，即"arg0"、"arg1"这样
     */
    private List<String> getArgNames(Constructor<?> constructor) {
      List<String> paramNames = new ArrayList<>();
      List<String> actualParamNames = null;
      final Annotation[][] paramAnnotations = constructor.getParameterAnnotations();
      int paramCount = paramAnnotations.length;
      for (int paramIndex = 0; paramIndex < paramCount; paramIndex++) {
        String name = null;
        for (Annotation annotation : paramAnnotations[paramIndex]) {
          if (annotation instanceof Param) {
            name = ((Param) annotation).value();
            break;
          }
        }
        if (name == null && resultMap.configuration.isUseActualParamName()) {
          if (actualParamNames == null) {
            actualParamNames = ParamNameUtil.getParamNames(constructor);
          }
          if (actualParamNames.size() > paramIndex) {
            name = actualParamNames.get(paramIndex);
          }
        }
        paramNames.add(name != null ? name : "arg" + paramIndex);
      }
      return paramNames;
    }
  }

  public String getId() {
    return id;
  }

  public boolean hasNestedResultMaps() {
    return hasNestedResultMaps;
  }

  public boolean hasNestedQueries() {
    return hasNestedQueries;
  }

  public Class<?> getType() {
    return type;
  }

  public List<ResultMapping> getResultMappings() {
    return resultMappings;
  }

  public List<ResultMapping> getConstructorResultMappings() {
    return constructorResultMappings;
  }

  public List<ResultMapping> getPropertyResultMappings() {
    return propertyResultMappings;
  }

  public List<ResultMapping> getIdResultMappings() {
    return idResultMappings;
  }

  public Set<String> getMappedColumns() {
    return mappedColumns;
  }

  public Set<String> getMappedProperties() {
    return mappedProperties;
  }

  public Discriminator getDiscriminator() {
    return discriminator;
  }

  public void forceNestedResultMaps() {
    hasNestedResultMaps = true;
  }

  public Boolean getAutoMapping() {
    return autoMapping;
  }

}
