package org.cdisource.beancontainer;

import java.util.Properties;
import java.util.ServiceLoader;

/**
 * Thread safe Bean Container Manager that provides access to the CDI
 * implementation on the classpath by returning an instance of the
 * {@link BeanContainer} that represents that implementation.
 * <p>
 * You can swap out the BeanContainer just by including the BeanContainer jar
 * file for a specific implementation in your project.
 * </p>
 * <p>
 * We look for a file under classpath/META-INF/services called
 * org.cdisource.beancontainer.BeanContainer.
 * </p>
 * This file has one of three values:
 * <ol>
 * <li>org.cdisource.beancontainer.ResinBeanContainer</li>
 * <li>org.cdisource.beancontainer.OpenWebBeansBeanContainer</li>
 * <li>org.cdisource.beancontainer.WeldBeanContainer</li>
 * </ol>
 * 
 * 
 * <p>
 * You can also override which container is used using a System property
 * 
 * </p>
 * 
 * <code>$ java -Dorg.cdisource.beancontainer.BeanContainer=org.cdisource.beancontainer.ResinBeanContainer</code>
 * 
 * <p>
 * </p>
 * 
 * @see java.util.ServiceLoader.
 * 
 * @author Rick Hightower
 * @author Andy Gibson
 * 
 */
public class BeanContainerManager {

	/** Property name that we use to look up the bean container override. */
	public static String PROP_NAME = "org.cdi.advocacy.BeanContainer";

	/**
	 * Holds a singleton instance of a {@link BeanContainer}
	 */
	private static BeanContainer instance;

	/**
	 * Thread safe method to create and initialize the {@link BeanContainer}
	 * implementation on the classpath.
	 * 
	 */
	public static void initialize() {
		initialize(System.getProperties());
	}

	/**
	 * Thread safe method to create and initialize the {@link BeanContainer}
	 * implementation on the classpath.
	 * 
	 * @param properties
	 *            Properties to use for initialization
	 */
	public synchronized static void initialize(Properties properties) {
		if (instance != null) {
			return;
		}
		startUpInstance(properties);

	}

	/**
	 * Get the bean container instance using system properties. If this method
	 * is called before one of the initialize methods is called, then the
	 * manager creates and initializes a {@link BeanContainer} instance to
	 * return.
	 * <p/>
	 * Auto initializing this way means that we can call the getter from
	 * anywhere without being totally concerned about who or where the container
	 * is initialized.
	 * 
	 * @see BeanContainerManager.PROP_NAME
	 * @see {@link BeanContainerManager#initialize()}
	 * */
	public static BeanContainer getInstance() {
		if (instance == null) {
			initialize();
		}
		return instance;
	}

	/**
	 * Thread safe method to create a new instance of a {@link BeanContainer}
	 * and start it up and make it available through the
	 * <code>getInstance()</code> method.
	 * 
	 * @param properties
	 *            Properties to use for initialization
	 */
	private synchronized static void startUpInstance(Properties properties) {
		// double check that the instance is null, someone might have created
		// it while we were entering this method.
		if (instance != null) {
			return;
		}
		instance = generateInstance(properties);
		if (instance != null) {
			instance.start();
		}
	}

	/**
	 * Internal method to create a new instance of a {@link BeanContainer}
	 * implementation. The returned container would not have been initialized
	 * yet.
	 * 
	 * @param properties
	 *            Properties to use for initialization
	 * @return new instance of a {@link BeanContainer} implementation which has
	 *         not yet been started.
	 */
	private static BeanContainer generateInstance(Properties properties) {
		try {
			/* The property should override the ServiceLoader if found. */
			String beanContainerClassName = properties.getProperty(PROP_NAME);

			/* If the property was not found, use the service loader. */
			if (beanContainerClassName == null) {
				ServiceLoader<BeanContainer> instances = ServiceLoader
						.load(BeanContainer.class);
				if (instances.iterator().hasNext()) {
					return instances.iterator().next();

				}
			}

			/*
			 * If class property not found in the passed properties, then Resin
			 * is the default for now, we may switch to the RI.
			 */
			beanContainerClassName = beanContainerClassName != null ? beanContainerClassName
					: "org.cdisource.beancontainer.WeldBeanContainer";

			/*
			 * Get the classloader associated with the current webapp and not
			 * the global classloader
			 */
			ClassLoader contextClassLoader = Thread.currentThread()
					.getContextClassLoader();
			Class<?> clazz = Class.forName(beanContainerClassName, true,
					contextClassLoader);
			return (BeanContainer) clazz.newInstance();

		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	/**
	 * Thread safe method to shutdown the {@link BeanContainer} instance and
	 * invalidate it. Further calls to get an instance or initialize will create
	 * a new instance.
	 */
	public static synchronized void shutdown() {
		if (instance != null) {
			instance.stop();
			instance = null;
		}
	}
}
