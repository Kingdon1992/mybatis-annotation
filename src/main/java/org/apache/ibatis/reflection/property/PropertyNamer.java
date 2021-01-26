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
package org.apache.ibatis.reflection.property;

import java.util.Locale;

import org.apache.ibatis.reflection.ReflectionException;

/**
 * @author Clinton Begin
 */
public final class PropertyNamer {

  private PropertyNamer() {
    // Prevent Instantiation of Static Class
  }

  public static String methodToProperty(String name) {
    // is开头的，是boolean类型域的get方法，特殊处理
    if (name.startsWith("is")) {
      name = name.substring(2);
    } else if (name.startsWith("get") || name.startsWith("set")) {
      name = name.substring(3);
    } else {
      throw new ReflectionException("Error parsing property name '" + name + "'.  Didn't start with 'is', 'get' or 'set'.");
    }

    /*
     当去除方法名称开头的is、get、set字样后
     ① 如果长度为1，直接变为小写，比如getA，isA，setA，最后得出[name]值都是a
     ② 如果长度大于1，也就是起码有2位，如果第二位不是大写，才会将第一位变为小写
      - getAb，isAb，setAb，最后得出[name]值是ab
      - getAB，isAB，setAB，最后得出[name]值是AB
     */
    if (name.length() == 1 || (name.length() > 1 && !Character.isUpperCase(name.charAt(1)))) {
      name = name.substring(0, 1).toLowerCase(Locale.ENGLISH) + name.substring(1);
    }

    return name;
  }

  public static boolean isProperty(String name) {
    return isGetter(name) || isSetter(name);
  }

  /**
   * 判断是否为get方法，分为两种类型
   * ①boolean类型以is开头
   * ②其他类型以get开头
   */
  public static boolean isGetter(String name) {
    return (name.startsWith("get") && name.length() > 3) || (name.startsWith("is") && name.length() > 2);
  }

  /**
   * 判断set方法的以set开头
   */
  public static boolean isSetter(String name) {
    return name.startsWith("set") && name.length() > 3;
  }

}
