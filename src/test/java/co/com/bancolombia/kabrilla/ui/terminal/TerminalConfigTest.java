package co.com.bancolombia.kabrilla.ui.terminal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Lógica de la terminal sin UI: comando del shell y contexto de kubectl.
 */
class TerminalConfigTest {

    @TempDir
    Path dir;

    @Test
    void parsesShellCommandWithQuotedPaths() {
        assertEquals(List.of("powershell.exe", "-NoLogo"), ShellCommand.parse(null));
        assertEquals(List.of("pwsh.exe"), ShellCommand.parse("  pwsh.exe "));
        assertEquals(List.of("C:/Program Files/Git/bin/bash.exe", "--login", "-i"),
                     ShellCommand.parse("\"C:/Program Files/Git/bin/bash.exe\" --login -i"));
        assertThrows(IllegalArgumentException.class, () -> ShellCommand.parse("\"C:/sin cerrar"));
        assertEquals("bash", ShellCommand.displayName(List.of("C:\\Program Files\\Git\\bin\\bash.exe")));
    }

    @Test
    void readsCurrentContextWithShortEksNameAndNamespace() throws Exception {
        Path kubeconfig = write("""
                current-context: arn:aws:eks:us-east-1:123:cluster/eks-ecosistemas-dev
                contexts:
                  - name: otro
                    context: {cluster: otro-cluster}
                  - name: arn:aws:eks:us-east-1:123:cluster/eks-ecosistemas-dev
                    context:
                      cluster: arn:aws:eks:us-east-1:123:cluster/eks-ecosistemas-dev
                      namespace: pagos-qa
                """);

        KubeContext context = KubeContext.read(kubeconfig);

        assertEquals("eks-ecosistemas-dev", context.cluster());
        assertEquals("eks-ecosistemas-dev · ns pagos-qa", context.describe());
    }

    @Test
    void explainsWhenThereIsNoContext() throws Exception {
        assertEquals("sin contexto actual", KubeContext.read(write("contexts: []\n")).describe());
        assertNull(KubeContext.read(dir.resolve("no-existe")).cluster());
    }

    @Test
    void resolvesKubeconfigLikeKubectl() {
        Path home = Path.of("home");
        assertEquals(Path.of("C:/mi/config"), KubeContext.kubeconfigPath("C:/mi/config", "otro", home));
        assertEquals(Path.of("primero"),
                     KubeContext.kubeconfigPath("", "primero" + File.pathSeparator + "segundo", home));
        assertEquals(home.resolve(".kube").resolve("config"), KubeContext.kubeconfigPath(null, null, home));
    }

    private Path write(String yaml) throws Exception {
        Path file = dir.resolve("config-" + System.nanoTime());
        Files.writeString(file, yaml);
        return file;
    }
}
