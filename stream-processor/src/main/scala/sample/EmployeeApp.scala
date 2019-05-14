package sample

import java.time.ZonedDateTime
import java.util.Properties

import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.{Serde, Serdes}
import org.apache.kafka.streams.kstream.Printed
import org.apache.kafka.streams.scala.ImplicitConversions._
import org.apache.kafka.streams.scala.kstream.{Consumed, Joined, KStream, KTable}
import org.apache.kafka.streams.{KafkaStreams, KeyValue, StreamsBuilder, StreamsConfig}

case class CustomerKey(emp_no: Long)
case class Customer(emp_no: Long, from_date: Long, first_name: String, last_name: String, gender: String, hire_date: Long, birth_date: Long)
case class CustomerJobDescription(emp_no: Long, job_description: String)

case class SalaryKey(emp_no: Long, from_date: Long)
case class Salary(emp_no: Long, from_date: Long, salary: Long, to_date: Long)

case class FullCustomer(emp_no: Long, first_name: String, last_name: String, salary: Long)
case class FullCustomerJobDescription(emp_no: Long, first_name: String, job_description: String)

object EmployeeApp {
  def main(args: Array[String]): Unit = {

    val employeeKeySerdes: Serde[CustomerKey] = Serdes.serdeFrom(new JsonPojoSerializer[CustomerKey](), JsonHybridDeserializer[CustomerKey])
    val employeeSerdes: Serde[Customer] = Serdes.serdeFrom(new JsonPojoSerializer[Customer](), JsonHybridDeserializer[Customer])
    val employeeJobDescriptionSerdes: Serde[CustomerJobDescription] = Serdes.serdeFrom(new JsonPojoSerializer[CustomerJobDescription](), JsonHybridDeserializer[CustomerJobDescription])

    val salaryKeySerdes: Serde[SalaryKey] = Serdes.serdeFrom(new JsonPojoSerializer[SalaryKey](), JsonHybridDeserializer[SalaryKey])
    val salarySerdes: Serde[Salary] = Serdes.serdeFrom(new JsonPojoSerializer[Salary](), JsonHybridDeserializer[Salary])

    implicit val joined = Joined.`with`(employeeKeySerdes, salarySerdes, employeeSerdes)
    implicit val joinedJobDescription = Joined.`with`(employeeKeySerdes, employeeJobDescriptionSerdes, employeeSerdes)

    val p = new Properties()

    p.put(StreamsConfig.APPLICATION_ID_CONFIG, "employee-aggregates-app")
    p.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "kafka:9092")
    p.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")

    val builder = new StreamsBuilder()

    val employeeTable: KTable[CustomerKey, Customer] = builder.table("db_emp.employees.employees", Consumed.`with`(employeeKeySerdes, employeeSerdes))

    val salariesTable: KStream[CustomerKey, Salary] = builder
      .stream("db_emp.employees.salaries", Consumed.`with`(salaryKeySerdes, salarySerdes))
      .map[CustomerKey, Salary]((key: SalaryKey, value: Salary) => new KeyValue[CustomerKey, Salary](CustomerKey(emp_no = key.emp_no), value))

    val employeeWithSalaryStream = salariesTable.join(employeeTable) { case (salary, customer) =>
      FullCustomer(customer.emp_no, customer.first_name, customer.last_name, salary.salary)
    }

    val employeeJobDescriptionStream: KTable[CustomerKey, CustomerJobDescription] = builder.table("db_emp.employees.employee_job_description", Consumed.`with`(employeeKeySerdes, employeeJobDescriptionSerdes))

    val employeeWithJobDescriptionStream = employeeJobDescriptionStream.join(employeeTable) { case (jobDescription, customer) =>
      FullCustomerJobDescription(customer.emp_no, customer.first_name, jobDescription.job_description)
    }

    employeeWithSalaryStream.print(Printed.toSysOut())
    employeeWithJobDescriptionStream.toStream.print(Printed.toSysOut())

    val streams = new KafkaStreams(builder.build(), p)
    streams.start()

    Runtime.getRuntime.addShutdownHook(new Thread(() => streams.close))
  }

}

