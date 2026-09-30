package io.github.querytemplate;

import java.util.Collection;

/**
 * Auxiliary class used only for the example in {@link QueryTemplateTest}.
 */
public class MyFilter {

	public static final class FormalName {
		private String firstName;
		private String lastName;
		public FormalName(
			String firstName,
			String lastName) {
			super();
			this.firstName = firstName;
			this.lastName = lastName;
		}
		public String getFirstName() {
			return firstName;
		}
		public void setFirstName(String firstName) {
			this.firstName = firstName;
		}
		public String getLastName() {
			return lastName;
		}
		public void setLastName(String lastName) {
			this.lastName = lastName;
		}
	}
	
    private String filterPrp1;
    private String filterPrp2;
    private String filterPrp3;
    private Collection<String> filterPrp4;
    private String filterPrp5;
    private MyProperty filterPrp6;
    private Collection<Integer> filterPrp7;
    private Integer[] filterPrp8;
    private FormalName formalName;
    private FormalName[] formalNamesArr;

	public MyFilter() {
        super();
    }

    public MyFilter(String filterPrp1, String filterPrp2, String filterPrp3, Collection<String> filterPrp4,
            String filterPrp5, MyProperty filterPrp6, Collection<Integer> filterPrp7, Integer[] filterPrp8) {
        super();
        this.filterPrp1 = filterPrp1;
        this.filterPrp2 = filterPrp2;
        this.filterPrp3 = filterPrp3;
        this.filterPrp4 = filterPrp4;
        this.filterPrp5 = filterPrp5;
        this.filterPrp6 = filterPrp6;
        this.filterPrp7 = filterPrp7;
        this.filterPrp8 = filterPrp8;
    }

    public MyFilter(
		String filterPrp1,
		String filterPrp2,
		String filterPrp3,
		Collection<String> filterPrp4,
		String filterPrp5,
		MyProperty filterPrp6,
		Collection<Integer> filterPrp7,
		Integer[] filterPrp8,
		FormalName formalName,
		FormalName[] formalNamesArr) {
		super();
		this.filterPrp1 = filterPrp1;
		this.filterPrp2 = filterPrp2;
		this.filterPrp3 = filterPrp3;
		this.filterPrp4 = filterPrp4;
		this.filterPrp5 = filterPrp5;
		this.filterPrp6 = filterPrp6;
		this.filterPrp7 = filterPrp7;
		this.filterPrp8 = filterPrp8;
		this.formalName = formalName;
		this.formalNamesArr = formalNamesArr;
	}

	public String getFilterPrp1() {
        return filterPrp1;
    }

    public void setFilterPrp1(String filterPrp1) {
        this.filterPrp1 = filterPrp1;
    }

    public String getFilterPrp2() {
        return filterPrp2;
    }

    public void setFilterPrp2(String filterPrp2) {
        this.filterPrp2 = filterPrp2;
    }

    public String getFilterPrp3() {
        return filterPrp3;
    }

    public void setFilterPrp3(String filterPrp3) {
        this.filterPrp3 = filterPrp3;
    }

    public Collection<String> getFilterPrp4() {
        return filterPrp4;
    }

    public void setFilterPrp4(Collection<String> filterPrp4) {
        this.filterPrp4 = filterPrp4;
    }

    public String getFilterPrp5() {
        return filterPrp5;
    }

    public void setFilterPrp5(String filterPrp5) {
        this.filterPrp5 = filterPrp5;
    }

    public MyProperty getFilterPrp6() {
        return filterPrp6;
    }

    public void setFilterPrp6(MyProperty filterPrp6) {
        this.filterPrp6 = filterPrp6;
    }

    public Collection<Integer> getFilterPrp7() {
        return filterPrp7;
    }

    public void setFilterPrp7(Collection<Integer> filterPrp7) {
        this.filterPrp7 = filterPrp7;
    }

    public Integer[] getFilterPrp8() {
        return filterPrp8;
    }

    public void setFilterPrp8(Integer[] filterPrp8) {
        this.filterPrp8 = filterPrp8;
    }

	public FormalName getFormalName() {
		return formalName;
	}

	public void setFormalName(FormalName formalName) {
		this.formalName = formalName;
	}

	public FormalName[] getFormalNamesArr() {
		return formalNamesArr;
	}

	public void setFormalNamesArr(FormalName[] formalNamesArr) {
		this.formalNamesArr = formalNamesArr;
	}

	public boolean isFilterPrp1AndfilterPrp2() {
    	if (this.getFilterPrp1() == null) {
    		return false;
    	}
    	if (this.getFilterPrp1().isEmpty()) {
    		return false;
    	}
		if (this.getFilterPrp2() == null) {
			return false;
		}
		if (this.getFilterPrp2().isEmpty()) {
			return false;
		}
		return true;
	}
}
