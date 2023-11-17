package com.llamalad7.mixinextras.utils.info;

import com.llamalad7.mixinextras.service.MixinExtrasService;

import java.io.*;

public class ExtraMixinInfoSerializer {
    public static void serialize(ExtraMixinInfo info, OutputStream file) {
        try (ObjectOutputStream output = new ObjectOutputStream(file)) {
            output.writeObject(info);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write extra mixin info: ", e);
        }
    }

    public static ExtraMixinInfo deSerialize(InputStream file) {
        try (ObjectInputStream input = new RelocatingObjectInputStream(file)) {
            return (ExtraMixinInfo) input.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException("Failed to read extra mixin info: ", e);
        }
    }

    private static class RelocatingObjectInputStream extends ObjectInputStream {
        public RelocatingObjectInputStream(InputStream in) throws IOException {
            super(in);
        }

        @Override
        protected ObjectStreamClass readClassDescriptor() throws IOException, ClassNotFoundException {
            ObjectStreamClass result = super.readClassDescriptor();
            String ourName = MixinExtrasService.getInstance().changePackageToOurs(result.getName());
            return ObjectStreamClass.lookup(Class.forName(ourName));
        }
    }
}
