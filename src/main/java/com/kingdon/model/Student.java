package com.kingdon.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

/**
 * @author : wangqingsong
 * @since : 2021-01-25 14:52:59
 */
@Setter
@Getter
@ToString
@NoArgsConstructor
public class Student {
  private int id;
  private String name;
  private String fatherName;
  private Teacher headTeacher;
  private List<Teacher> teachers;

  public Student(int id, String fatherName, String name) {
    this.id = id;
    this.name = name;
    this.fatherName = fatherName;
  }
}
