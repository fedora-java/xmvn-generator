#include <jni.h>
#include <rpm/rpmmacro.h>

JNIEXPORT jstring JNICALL
Java_org_fedoraproject_xmvn_generator_stub_RpmBuildContext_eval(JNIEnv *J, jobject this, jstring expr)
{
  (void)this;
  const char *expr1 = (*J)->GetStringUTFChars(J, expr, NULL);
  char *val1 = rpmExpand(expr1, NULL);
  (*J)->ReleaseStringUTFChars(J, expr, expr1);
  jstring val = (*J)->NewStringUTF(J, val1);
  free(val1);
  return val;
}
