package test;

import com.kingdon.model.Student;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * @author : wangqingsong
 * @since : 2020-11-22 13:10:13
 */
public class App {

  @Test
  public void baseTest() throws IOException {
    InputStream inputStream = Resources.getResourceAsStream("mybatis.xml");
    SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
    SqlSession sqlSession = sqlSessionFactory.openSession();
    List<Student> student = sqlSession.selectList("com.kingdon.dao.StudentDao.getById", 1);
    System.out.println(student);
  }

  @Test
  public void mapTest() throws IOException {
    InputStream inputStream = Resources.getResourceAsStream("mybatis.xml");
    SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
    SqlSession sqlSession = sqlSessionFactory.openSession();
    Map student = sqlSession.selectOne("com.kingdon.dao.StudentDao.getByIdWithMap", 1);
    System.out.println(student);
  }

  @Test
  public void constructorTest() throws IOException {
    InputStream inputStream = Resources.getResourceAsStream("mybatis.xml");
    SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
    SqlSession sqlSession = sqlSessionFactory.openSession();
    Student student = sqlSession.selectOne("com.kingdon.dao.StudentDao.getByIdWithConstructor", 1);
    System.out.println(student);
  }
}
