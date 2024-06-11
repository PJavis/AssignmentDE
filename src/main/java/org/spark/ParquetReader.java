package org.spark;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;

import static org.apache.spark.sql.functions.sum;

public class ParquetReader {
    public static void main(String[] args) {
        SparkSession spark = SparkSession.builder()
                .appName("Read Parquet from HDFS and Aggregate Activities")
                .config("spark.master", "local")
                .getOrCreate();

        String studentFilePath = "hdfs://namenode:8020/input/.danh_sach_sv_de.csv";

        // Định nghĩa schema cho tệp CSV
        StructType schema = new StructType(new StructField[]{
                DataTypes.createStructField("student_code", DataTypes.IntegerType, false),
                DataTypes.createStructField("student_name", DataTypes.StringType, false)
        });

        // Đọc dữ liệu từ tệp CSV và sử dụng schema đã định nghĩa
        Dataset<Row> studentDf = spark.read()
                .format("csv")
                .schema(schema)
                .option("header", "false")
                .load(studentFilePath);

        studentDf.show();

        String hdfsPath = "hdfs://namenode:8020/raw_zone/fact/activity";
        Dataset<Row> df = spark.read().parquet(hdfsPath);

        Dataset<Row> aggregatedDF = df.groupBy("student_code", "activity", "timestamp")
                .agg(sum("numberOfFile").as("totalFile"))
                .orderBy("timestamp", "student_code", "activity")
                .select("timestamp", "student_code", "activity", "totalFile");

        aggregatedDF.show();

        String outputHdfsPath = "hdfs://namenode:8020/output/aggregated_activities";
        aggregatedDF.write()
                .mode("overwrite")
                .option("header", "true")
                .csv(outputHdfsPath);

        spark.stop();
    }
}