package com.kingdon.typehandler;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author : wangqingsong
 * @since : 2021-01-25 15:23:04
 */
public class NameTypeHandler extends BaseTypeHandler<String> {
  @Override
  public void setNonNullParameter(PreparedStatement ps, int i, String parameter, JdbcType jdbcType) throws SQLException {
    System.out.println();
  }

  @Override
  public String getNullableResult(ResultSet rs, String columnName) throws SQLException {
    String data = rs.getString(columnName);
    if (columnName.equals("father_name")){
      return "父亲："+ data;
    }
    return data;
  }

  @Override
  public String getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
    System.out.println();
    return null;
  }

  @Override
  public String getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
    System.out.println();
    return null;
  }
}
