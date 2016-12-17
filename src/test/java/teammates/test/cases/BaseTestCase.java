package teammates.test.cases;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Arrays;

import org.testng.AssertJUnit;

import teammates.common.datatransfer.DataBundle;
import teammates.common.util.FieldValidator;
import teammates.common.util.JsonUtils;
import teammates.logic.backdoor.BackDoorLogic;
import teammates.test.driver.TestProperties;
import teammates.test.util.FileHelper;

/** Base class for all test cases */
public class BaseTestCase {


    /**
     * Test Segment divider. Used to divide a test case into logical sections.
     * The weird name is for easy spotting.
     * 
     * @param description
     *            of the logical section. This will be printed.
     */
    // CHECKSTYLE.OFF:AbbreviationAsWordInName|MethodName the weird name is for easy spotting.
    public static void ______TS(String description) {
        print(" * " + description);
    }
    // CHECKSTYLE.ON:AbbreviationAsWordInName|MethodName

    public static void printTestCaseHeader() {
        print("[TestCase]---:" + Thread.currentThread().getStackTrace()[2].getMethodName());
    }

    public static void printTestClassHeader() {
        print("[============================="
                + Thread.currentThread().getStackTrace()[2].getClassName()
                + "=============================]");
    }

    public static void printTestClassFooter() {
        print(Thread.currentThread().getStackTrace()[2].getClassName() + " completed");
    }

    protected static void print(String message) {
        System.out.println(message);
    }

    /**
     * Creates a DataBundle as specified in typicalDataBundle.json
     */
    protected static DataBundle getTypicalDataBundle() {
        return loadDataBundle("/typicalDataBundle.json");
    }
    
    protected static DataBundle loadDataBundle(String pathToJsonFileParam) {
        try {
            String pathToJsonFile = (pathToJsonFileParam.startsWith("/") ? TestProperties.TEST_DATA_FOLDER : "")
                                  + pathToJsonFileParam;
            String jsonString = FileHelper.readFile(pathToJsonFile);
            return JsonUtils.fromJson(jsonString, DataBundle.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Creates in the datastore a fresh copy of data in typicalDataBundle.json
     */
    protected static void restoreTypicalDataInDatastore() throws Exception {
        BackDoorLogic backDoorLogic = new BackDoorLogic();
        DataBundle dataBundle = getTypicalDataBundle();
        backDoorLogic.persistDataBundle(dataBundle);
    }

    protected static void removeAndRestoreTypicalDataInDatastore() throws Exception {
        BackDoorLogic backDoorLogic = new BackDoorLogic();
        DataBundle dataBundle = getTypicalDataBundle();
        backDoorLogic.deleteExistingData(dataBundle);
        backDoorLogic.persistDataBundle(dataBundle);
    }
    
    protected static void removeTypicalDataInDatastore() {
        BackDoorLogic backDoorLogic = new BackDoorLogic();
        DataBundle dataBundle = getTypicalDataBundle();
        backDoorLogic.deleteExistingData(dataBundle);
    }
    
    /**
     * Creates in the datastore a fresh copy of data in the given json file
     */
    protected static void restoreDatastoreFromJson(String pathToJsonFile) throws Exception {
        BackDoorLogic backDoorLogic = new BackDoorLogic();
        DataBundle dataBundle = loadDataBundle(pathToJsonFile);
        backDoorLogic.persistDataBundle(dataBundle);
    }

    protected static void removeAndRestoreDatastoreFromJson(String pathToJsonFile) throws Exception {
        BackDoorLogic backDoorLogic = new BackDoorLogic();
        DataBundle dataBundle = loadDataBundle(pathToJsonFile);
        backDoorLogic.deleteExistingData(dataBundle);
        backDoorLogic.persistDataBundle(dataBundle);
    }

    protected void signalFailureToDetectException(String... messages) {
        throw new RuntimeException("Expected exception not detected." + Arrays.toString(messages));
    }

    protected void ignoreExpectedException() {
        assertTrue(true);
    }
    
    protected static void ignorePossibleException() {
        assertTrue(true);
    }
    
    /**
     * Invokes the method named {@code methodName} as defined in the {@code definingClass}.
     * @param definingClass     the class which defines the method
     * @param methodName
     * @param parameterTypes    the parameter types of the method,
     *                          which must be passed in the same order defined in the method
     * @param invokingObject    the object which invokes the method, can be {@code null} if the method is static
     * @param args              the arguments to be passed to the method invocation
     */
    protected static Object invokeMethod(Class<?> definingClass, String methodName, Class<?>[] parameterTypes,
                                         Object invokingObject, Object[] args)
            throws ReflectiveOperationException {
        Method method = definingClass.getDeclaredMethod(methodName, parameterTypes);
        method.setAccessible(true);
        return method.invoke(invokingObject, args);
    }
    
    protected static String getPopulatedErrorMessage(String messageTemplate, String userInput,
                                                     String fieldName, String errorReason)
            throws ReflectiveOperationException {
        return getPopulatedErrorMessage(messageTemplate, userInput, fieldName, errorReason, 0);
    }

    protected static String getPopulatedErrorMessage(String messageTemplate, String userInput,
                                                     String fieldName, String errorReason, int maxLength)
            throws ReflectiveOperationException {
        return (String) invokeMethod(FieldValidator.class, "getPopulatedErrorMessage",
                                     new Class<?>[] { String.class, String.class, String.class, String.class, int.class },
                                     null, new Object[] { messageTemplate, userInput, fieldName, errorReason, maxLength });
    }

    /*
     * Here are some of the most common assertion methods provided by JUnit.
     * They are copied here to prevent repetitive importing in test classes.
     */

    protected static void assertTrue(boolean condition) {
        AssertJUnit.assertTrue(condition);
    }
    
    protected static void assertTrue(String message, boolean condition) {
        AssertJUnit.assertTrue(message, condition);
    }
    
    protected static void assertFalse(boolean condition) {
        AssertJUnit.assertFalse(condition);
    }
    
    protected static void assertFalse(String message, boolean condition) {
        AssertJUnit.assertFalse(message, condition);
    }
    
    protected static void assertEquals(String expected, String actual) {
        AssertJUnit.assertEquals(expected, actual);
    }
    
    protected static void assertEquals(String message, String expected, String actual) {
        AssertJUnit.assertEquals(message, expected, actual);
    }
    
    protected static void assertEquals(int expected, int actual) {
        AssertJUnit.assertEquals(expected, actual);
    }
    
    protected static void assertEquals(String message, int expected, int actual) {
        AssertJUnit.assertEquals(message, expected, actual);
    }
    
    protected static void assertEquals(boolean expected, boolean actual) {
        AssertJUnit.assertEquals(expected, actual);
    }
    
    protected static void assertEquals(String message, boolean expected, boolean actual) {
        AssertJUnit.assertEquals(message, expected, actual);
    }
    
    protected static void assertEquals(long expected, long actual) {
        AssertJUnit.assertEquals(expected, actual);
    }
    
    protected static void assertEquals(double expected, double actual, double delta) {
        AssertJUnit.assertEquals(expected, actual, delta);
    }
    
    protected static void assertEquals(Object expected, Object actual) {
        AssertJUnit.assertEquals(expected, actual);
    }
    
    protected static void assertNull(Object object) {
        AssertJUnit.assertNull(object);
    }
    
    protected static void assertNull(String message, Object object) {
        AssertJUnit.assertNull(message, object);
    }
    
    protected static void assertNotNull(Object object) {
        AssertJUnit.assertNotNull(object);
    }
    
    protected static void assertNotNull(String message, Object object) {
        AssertJUnit.assertNotNull(message, object);
    }
    
    protected static void fail(String message) {
        AssertJUnit.fail(message);
    }
    
}
