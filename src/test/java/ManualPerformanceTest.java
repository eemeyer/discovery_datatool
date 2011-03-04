import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Logger;

import org.apache.commons.lang.time.StopWatch;

import com.mysql.jdbc.Driver;
import com.t11e.discovery.datatool.StopWatchHelper;

public class ManualPerformanceTest
{
  private static final Logger logger = Logger.getLogger(ManualPerformanceTest.class.getName());
  public static void main(final String[] args)
    throws Exception
  {
    new ManualPerformanceTest().doTest();
  }

  private Driver driver;

  private void doTest()
    throws SQLException
  {
    driver = new com.mysql.jdbc.Driver();

    getConnection().close();

    timeOpenCloseConnection();
    timeQueryAlone();
    timeQueryWithSubquery();
  }

  private void timeQueryWithSubquery()
    throws SQLException
  {
    final int numLoops = 10;
    final StopWatch watch = StopWatchHelper.startTimer(true);
    for (int i = 0; i < numLoops; i++)
    {
      final Connection con = getConnection();
      final PreparedStatement stmt = con.prepareStatement(
        "select\n" +
          "    'Movie' as content_type,\n" +
          "    'TMS' as \"Movie DB\",\n" +
          "    concat(tms_movies.TMS_TMSId, '-TMS') as \"movie_id\",\n" +
          "    tms_movies.TMS_TMSId as \"tms_id\",\n" +
          "    tms_movies.title as \"Title\",\n" +
          "    if((coalesce(tms_movies.release_year,0)>0), tms_movies.release_year,null) as Release_Year,\n" +
          "    if(length(tms_movies.synopsis),tms_movies.synopsis, null) as \"Synopsis\"\n" +
          "    from tms_movies tms_movies");
      final PreparedStatement subqStmt = con.prepareStatement(
        "            select tr.TMS_rating\n" +
          "            from tms_lmovieratings tr\n" +
          "            where tr.TMS_TMSId = ?\n" +
          "             and tr.TMS_ratingsBody = 'Motion Picture Association of America'\n" +
          "             and tr.TMS_area = 'United States'");
      final ResultSet resultSet = stmt.executeQuery();
      while (resultSet.next())
      {
        logger.finest(
          resultSet.getString(1) +
            resultSet.getString(2) +
            resultSet.getString(3) +
            resultSet.getString(4) +
            resultSet.getString(5) +
            resultSet.getString(6));
        subqStmt.setString(1, resultSet.getString(4));
        final ResultSet subqResultSet = subqStmt.executeQuery();
        while (subqResultSet.next())
        {
          logger.finest(subqResultSet.getString(1));
        }
        subqResultSet.close();
      }
      subqStmt.close();
      stmt.close();
      resultSet.close();
      con.close();
    }
    watch.stop();
    System.out.println("Time to execute query with subquery " + numLoops + " times: " + watch + " avg: "
      + watch.getTime() * 1.0 / numLoops + "ms");

  }

  private void timeQueryAlone()
    throws SQLException
  {
    final int numLoops = 10;
    final StopWatch watch = StopWatchHelper.startTimer(true);
    for (int i = 0; i < numLoops; i++)
    {
      final Connection con = getConnection();
      final PreparedStatement stmt = con.prepareStatement(
        "select\n" +
          "    'Movie' as content_type,\n" +
          "    'TMS' as \"Movie DB\",\n" +
          "    concat(tms_movies.TMS_TMSId, '-TMS') as \"movie_id\",\n" +
          "    tms_movies.title as \"Title\",\n" +
          "    if((coalesce(tms_movies.release_year,0)>0), tms_movies.release_year,null) as Release_Year,\n" +
          "    if(length(tms_movies.synopsis),tms_movies.synopsis, null) as \"Synopsis\"\n" +
          "    from tms_movies tms_movies");
      final ResultSet resultSet = stmt.executeQuery();
      while (resultSet.next())
      {
        logger.finest(
          resultSet.getString(1) +
            resultSet.getString(2) +
            resultSet.getString(3) +
            resultSet.getString(4) +
            resultSet.getString(5) +
            resultSet.getString(6));
      }
      stmt.close();
      resultSet.close();
      con.close();
    }
    watch.stop();
    System.out.println("Time to execute simple query " + numLoops + " times: " + watch + " avg: "
      + watch.getTime() * 1.0 / numLoops + "ms");
  }

  private void timeOpenCloseConnection()
    throws SQLException
  {
    final int numLoops = 1000;
    final StopWatch watch = StopWatchHelper.startTimer(true);
    for (int i = 0; i < numLoops; i++)
    {
      getConnection().close();
    }
    watch.stop();
    System.out.println("Time to open and close a db connection " + numLoops + " times: " + watch + " avg: "
      + watch.getTime() * 1.0 / numLoops + "ms");
  }

  private Connection getConnection()
    throws SQLException
  {
    final Properties props = new Properties();
    props.setProperty("user", "epix");
    props.setProperty("password", "epix");
    return driver.connect("jdbc:mysql://localhost/epixcms", props);
  }

}
