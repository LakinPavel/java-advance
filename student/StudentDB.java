package info.kgeorgiy.ja.Laskin_Pavel.student;

import info.kgeorgiy.java.advanced.student.GroupName;
import info.kgeorgiy.java.advanced.student.Student;
import info.kgeorgiy.java.advanced.student.StudentQuery;

import java.util.*;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class StudentDB implements StudentQuery {

    public static final Comparator<Student> STUDENT_COMPARATOR = Comparator.comparing(Student::firstName)
            .thenComparing(Student::lastName)
            .thenComparing(Student::id);

    private <T> Stream<T> doMap(List<Student> students, Function<Student, T> info) {
        return students.stream().map(info);
    }

    private <T> List<T> getStudentInfo(List<Student> students, Function<Student, T> info) {
        return doMap(students, info).toList();
    }

    @Override
    public List<String> getFirstNames(List<Student> students) {
        return getStudentInfo(students, Student::firstName);
    }

    @Override
    public List<String> getLastNames(List<Student> students) {
        return getStudentInfo(students, Student::lastName);
    }

    @Override
    public List<GroupName> getGroupNames(List<Student> students) {
        return getStudentInfo(students, Student::groupName);
    }

    private String fullName(Student student) {
        return student.firstName() + " " + student.lastName();
    }

    @Override
    public List<String> getFullNames(List<Student> students) {
        return getStudentInfo(students, this::fullName);
    }

    @Override
    public Set<String> getDistinctFirstNames(List<Student> students) {
        return doMap(students, Student::firstName).collect(Collectors.toCollection(TreeSet::new));
    }

    @Override
    public String getMaxStudentFirstName(List<Student> students) {
        return students.stream()
                .max(Comparator.comparingInt(Student::id))
                .map(Student::firstName).orElse("");
    }

    private List<Student> sortStream(Stream<Student> streamStudents) {
        return streamStudents.sorted(STUDENT_COMPARATOR).toList();
    }

    private <T> Stream<Student> filterStream(Collection<Student> students, Function<Student, T> info, T comp) {
        return students.stream().filter(st -> info.apply(st).equals(comp));
    }

    @Override
    public List<Student> sortStudentsById(Collection<Student> students) {
        return students.stream().sorted(Comparator.comparingInt(Student::id)).toList();
    }

    @Override
    public List<Student> sortStudentsByName(Collection<Student> students) {
        return sortStream(students.stream());
    }

    @Override
    public List<Student> findStudentsByFirstName(Collection<Student> students, String name) {
        return getStudents(students, name);
    }

    private List<Student> getStudents(Collection<Student> students, String name) {
        return sortStream(filterStream(students, Student::firstName, name));
    }

    @Override
    public List<Student> findStudentsByLastName(Collection<Student> students, String name) {
        return sortStream(filterStream(students, Student::lastName, name));
    }

    @Override
    public List<Student> findStudentsByGroup(Collection<Student> students, GroupName group) {
        return sortStream(filterStream(students, Student::groupName, group));
    }

    @Override
    public Map<String, String> findStudentNamesByGroup(Collection<Student> students, GroupName group) {
        return filterStream(students, Student::groupName, group)
                .collect(Collectors.toMap(
                        Student::lastName, Student::firstName,
                        BinaryOperator.minBy(String::compareTo)));
    }
}
