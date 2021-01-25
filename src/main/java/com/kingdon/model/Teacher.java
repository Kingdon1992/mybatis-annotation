package com.kingdon.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * @author : wangqingsong
 * @since : 2021-01-25 14:53:29
 */
@Getter
@Setter
@ToString
public class Teacher {
  private int id;
  private String name;
  private int age;
  private String subject;
  private boolean headTeacher;
}
