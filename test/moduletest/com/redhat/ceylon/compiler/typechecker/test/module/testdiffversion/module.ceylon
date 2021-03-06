@error;
Module module {
    name = 'com.redhat.ceylon.compiler.typechecker.test.module.testdiffversion';
    version = '1';
    doc = "Test that multiple imports from different versions at the same level fail";
    license = 'http://www.apache.org/licenses/LICENSE-2.0.html';
    Import {
        name = 'com.redhat.ceylon.compiler.typechecker.test.module.c';
        version = '1';
    },
    Import {
        name = 'com.redhat.ceylon.compiler.typechecker.test.module.c';
        version = '2';
    }
}