package com.kingdon.dao;

import com.kingdon.model.Student;

import java.util.Map;

/**
 * @author : wangqingsong
 * @since : 2021-01-25 15:00:54
 */
public interface StudentDao {
  Student getById(int id);

  Map getByIdWithMap(int id);

  Student getByIdWithConstructor(int id);
}
