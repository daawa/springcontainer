[![](https://jitpack.io/v/ziv-zh/springcontainer.svg)](https://jitpack.io/#ziv-zh/springcontainer)

# SpringContainer

----------


`SpringContainer` is a `FrameLayout` that supports overscroll on both top and bottom. Feel free to put **any view(s)** in.

Based on that feature, it's very easy to do refreshing、 loading or page-switching etc.


If `spring` feature is disabled, it is just acts as a `FrameLayout`, if not, you can overscroll on top and bottom ends no matter what views are put in.

And SpringContainer does not interfere the horizontal scrolling of child views.

In the following demo, a HorizontalScrollView is put in a SpringContainer. 

![spring-and-support-horizontal](https://raw.githubusercontent.com/ziv-zh/notes/master/files/springcontainer_scroll-view.gif)

<br/><br/>
As said above, it is easy to expose a **PULL-TO-REFRESH** feature.

Just define a class that implements interface `ISpringView`, and provide an object of that type as the the spring container's header view. UI and animation can be totally customed according to different states notified by SpringContainer.

The following demo use `SampleHeaderView` provided in the package.
 
![top-pulling-to-refresh](https://raw.githubusercontent.com/ziv-zh/notes/master/files/springcontainer_pull-2-refresh.gif)

Like a pull-to-refresh feature, it is also convenient for you to come to a **PUSH-TO-LOAD-MORE** functionality. And the way to do that is all the same, except that the customed `ISpringView` object should be set as SpringContainer's footer view.

The following demo uses a `SampleFooterView` provided in the package. check the source code for more detail.
 
![bottom-overscroll-loading](https://raw.githubusercontent.com/ziv-zh/notes/master/files/springcontainer_load-more.gif)


SpringContainer supports **multi-touch**, you can use different fingers to **continuously** pull or push.

![multi-pointer-support](https://raw.githubusercontent.com/ziv-zh/notes/master/files/springcontainer_multi-touch.gif)


SpringContainer also supports changing states through code.

In the following demo, clicking ***refresh*** button makes SpringContainer transfer to **REFRESHING** state, and clicking ***load***, make it transfer to **LOADING**

![support-set-state](https://raw.githubusercontent.com/ziv-zh/notes/master/files/springcontainer_set-state.gif) 


If you wanna implement vetical page-switching , it is also easy. All the work you should do is provide you translation animation and a `ISpringView`, and you can turn to the `PageSwitchActivity` demo in the sample code.

![](https://raw.githubusercontent.com/ziv-zh/notes/master/files/springdemo_page_switch.gif)  


<br/><br/><br/>


# Usage

----------
## Add `SpringContainer` to your project:
### 1. for maven:
	

	<dependency>
  		<groupId>com.ziv.lib</groupId>
  		<artifactId>SpringContainer</artifactId>
  		<version>0.3.0</version>
  		<type>pom</type>
	</dependency>


### 2. for gradle:

	compile 'com.ziv.lib:SpringContainer:0.2.2'
	

If you use JitPack repository,
Add it in your root build.gradle at the end of repositories:

	allprojects {
		repositories {
			...
			maven { url 'https://jitpack.io' }
		}
	}
	


<br/><br/>

## Implements  ***ISpringView***

`ISpringView` is an interface:

	/**
 	 * Custom your header view or footer view by implements this interface
 	*/

	public interface ISpringView {

	    /**
	     * create a custom view to be displayed on top or bottom of SpringContainer, which can be updated or animated at different state
	     * @param springView SpringContainer's headerContainer or footerContainer
	     * @return a view
	     */
	    View onCreateSpringView(ViewGroup springView);

	    /**
	     * when the state of SpringContainer changed, this method would be called.
	     * @param old
	     * @param current
	     */
	    void onStateChanged(int old, int current);

	    /**
	     * when the height of SpringContainer's headerContainer changed, this method would be called
	     * @param cur  current height of SpringContainer's headerContainer
	     */
	    void onHeightChanged(int cur);
		
		/**
     		* invoked when user's finger is released from the SpringContainer
     		* @param springView
    		* @return true if you deal with {@param springView} yourself, else the SpringContainer will animate the {@param springView} to being gone.
     	*/
    	boolean onRelease(ViewGroup springView);
    
	}

Generally, you can leave all the methods empty, and SpringContainer would acts as the first demo. 

If you want to display a custom view , return one by implementing `View onCreateSpringView(ViewGroup springView)` . 

Forthermore, you can animate the view arbitrarily according to the height change of top or bottom area notified through `View onCreateSpringView(ViewGroup springView)`.

UI change or other action can also be made when the state of spring container changed, which would be seen in `void onStateChanged(int old, int current)` .

Check `SampleHeaderVeiw` and `SampleFooterView` for more details.

<br/>
## Set your customed spring view to SpringContainer
When you prepared a `ISpringView`, say `SampleFooterView`, just set it as a footer or header of SpringContainer:

	springContainer.setFooterView(new SampleFooterView());

<br/>
<br/>
# Java Docs

----------

 For more info, [check out the javadoc here](https://ziv-zh.github.io/notes/javadoc/SpringContainer0.2.2/index.html "https://ziv-zh.github.io/notes/javadoc/SpringContainer0.2.2/index.html")


<br/><br/>
#- . -

[contact me if any problem](mailto:mayzzw@126.com "contact me: mailto:mayzzw@126.com")